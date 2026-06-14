package com.meiya.skillsmap.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.FavoriteMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.service.impl.SkillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：Skill 下载次数累加（Sprint 40 修复）。
 *
 * <p>bug 现象：页面下载 .skill zip 与 skills-manager CLI 安装走同一个
 * {@code GET /api/skills/slug/{slug}/download} 端点，但 handler 内
 * 完全没累加任何计数，导致 installs 永远为 0。
 *
 * <p>修复策略：在 Service 层提供 {@link SkillService#incrementInstalls(Long)}，
 * 用 MyBatis-Plus {@code UpdateWrapper.setSql("installs = installs + 1")}
 * 走单条原子 SQL，Controller 在两个下载入口 best-effort 调用。
 *
 * <p>本测试覆盖 Service 层契约：
 * <ul>
 *   <li>正常 id：触发原子 SQL（setSql 含 installs = installs + 1 + eq id）</li>
 *   <li>id 不存在（update 返回 0）：no-op，不抛异常</li>
 *   <li>id 为 null：连 DB 都不打，直接返回</li>
 * </ul>
 *
 * <p>测试风格与 {@link SkillUploadServiceTest} 对齐——不拉 Spring 上下文，
 * 直接 {@code new SkillServiceImpl(...)} 注入 mock。
 */
class SkillDownloadCounterTest {

    private SkillMapper skillMapper;
    private SkillServiceImpl service;

    private static final Long EXISTING_SKILL_ID = 42L;

    @BeforeEach
    void setUp() {
        skillMapper = mock(SkillMapper.class);
        // 其它依赖被构造函数要求注入，但 incrementInstalls 不使用它们
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        TagMapper tagMapper = mock(TagMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        FavoriteMapper favoriteMapper = mock(FavoriteMapper.class);
        SkillStorageService skillStorageService = mock(SkillStorageService.class);

        service = new SkillServiceImpl(
                categoryMapper, tagMapper, userMapper,
                favoriteMapper, skillStorageService);
        // ServiceImpl 父类持有 baseMapper 字段，需要用反射注入 mock 的 skillMapper
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", skillMapper);
    }

    @Test
    @DisplayName("incrementInstalls(existingId)：走单条原子 SQL，含 installs=installs+1 与 eq id")
    void shouldRunAtomicSqlIncrement() {
        // BaseMapper.update 返回 int：1 表示 1 行受影响
        when(skillMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.incrementInstalls(EXISTING_SKILL_ID);

        // 捕获 UpdateWrapper
        ArgumentCaptor<UpdateWrapper<Skill>> cap = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(skillMapper).update(isNull(), cap.capture());

        UpdateWrapper<Skill> wrapper = cap.getValue();
        // 关键：setSql 拼接出 "installs = installs + 1"（SQL set 片段）
        assertThat(wrapper.getSqlSet()).contains("installs = installs + 1");
        // WHERE 条件：eq id（MyBatis-Plus 用 #{ew.paramNameValuePairs.MPGENVAL1} 占位符防注入）
        assertThat(wrapper.getSqlSegment()).contains("id = #{ew.paramNameValuePairs.MPGENVAL1}");
        // 参数值：实际传入的 id 值在 paramNameValuePairs 里
        assertThat(wrapper.getParamNameValuePairs().values())
                .as("eq id 应携带传入的 skill id 作为参数")
                .contains(EXISTING_SKILL_ID);
    }

    @Test
    @DisplayName("incrementInstalls(id 不存在)：update 返回 0 → no-op，不抛异常")
    void shouldNoopWhenSkillNotFound() {
        when(skillMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(0);

        // 不应抛异常
        service.incrementInstalls(99999L);

        verify(skillMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("incrementInstalls(null)：连 DB 都不打，直接返回")
    void shouldNoopWhenIdIsNull() {
        service.incrementInstalls(null);

        // 关键：never() — 任何 update 调用都不应有
        verify(skillMapper, never()).update(any(), any(UpdateWrapper.class));
        verify(skillMapper, never()).update(any(Skill.class), any(UpdateWrapper.class));
    }
}
