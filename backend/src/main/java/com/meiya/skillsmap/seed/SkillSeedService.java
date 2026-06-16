package com.meiya.skillsmap.seed;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.meiya.skillsmap.config.SeedProperties;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.entity.Tag;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.SkillTagMapper;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.service.SkillStorageService;
import com.meiya.skillsmap.service.impl.TagServiceImpl;
import com.meiya.skillsmap.util.CategoryUtil;
import com.meiya.skillsmap.util.MarkdownFrontmatterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * 种子数据服务 (S04: SOC 职业分类版)
 * - 装 23 SOC 一级 + 96 sub-group = 119 类目
 * - 装 10 旧 domain → 新 SOC 的 slug 301 重定向
 * - 装 2 默认用户
 * - 扫本地 Skills 目录导入 skills（按 SOC code 重分类）
 */
@Service
public class SkillSeedService {

    private static final Logger log = LoggerFactory.getLogger(SkillSeedService.class);

    private final SeedProperties seedProperties;
    private final SkillMapper skillMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final SkillTagMapper skillTagMapper;
    private final UserMapper userMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final SkillStorageService skillStorageService;

    public SkillSeedService(SeedProperties seedProperties, SkillMapper skillMapper,
                            CategoryMapper categoryMapper, TagMapper tagMapper,
                            SkillTagMapper skillTagMapper, UserMapper userMapper,
                            org.springframework.jdbc.core.JdbcTemplate jdbc,
                            SkillStorageService skillStorageService) {
        this.seedProperties = seedProperties;
        this.skillMapper = skillMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.skillTagMapper = skillTagMapper;
        this.userMapper = userMapper;
        this.jdbc = jdbc;
        this.skillStorageService = skillStorageService;
    }

    /** S04: SOC 节点（23 一级 + 96 sub-group = 119） */
    private record SocNode(String code, String parentCode, String nameZh, String nameEn, String slug, int sortOrder) {}

    /** S18: USAGE 节点（12 一级 + 70 sub-group = 82） */
    private record UsageNode(String code, String parentCode, String nameZh, String nameEn, String slug, int sortOrder) {}

    private static final List<SocNode> SOC_TAXONOMY = List.of(
        new SocNode("#01", null, "计算机与数学类职业", "Computer and Mathematical", "01-计算机与数学类职业", 1),
        new SocNode("#02", null, "商业与金融运营类职业", "Business and Financial Operations", "02-商业与金融运营类职业", 2),
        new SocNode("#03", null, "艺术设计娱乐体育与媒体", "Arts Design Entertainment Sports and Media", "03-艺术设计娱乐体育与媒体", 3),
        new SocNode("#04", null, "办公室与行政支持类职业", "Office and Administrative Support", "04-办公室与行政支持类职业", 4),
        new SocNode("#05", null, "教育与图书馆类职业", "Educational Instruction and Library", "05-教育与图书馆类职业", 5),
        new SocNode("#06", null, "生命物理与社会科学类职业", "Life Physical and Social Science", "06-生命物理与社会科学类职业", 6),
        new SocNode("#07", null, "管理类职业", "Management", "07-管理类职业", 7),
        new SocNode("#08", null, "法律类职业", "Legal", "08-法律类职业", 8),
        new SocNode("#09", null, "销售及相关类职业", "Sales and Related", "09-销售及相关类职业", 9),
        new SocNode("#10", null, "建筑与工程类职业", "Architecture and Engineering", "10-建筑与工程类职业", 10),
        new SocNode("#11", null, "医疗从业者与技术类职业", "Healthcare Practitioners and Technical", "11-医疗从业者与技术类职业", 11),
        new SocNode("#12", null, "个人护理与服务类职业", "Personal Care and Service", "12-个人护理与服务类职业", 12),
        new SocNode("#13", null, "社区与社会服务类职业", "Community and Social Service", "13-社区与社会服务类职业", 13),
        new SocNode("#14", null, "军事特定职业", "Military Specific", "14-军事特定职业", 14),
        new SocNode("#15", null, "食品准备与餐饮服务类职业", "Food Preparation and Serving", "15-食品准备与餐饮服务类职业", 15),
        new SocNode("#16", null, "运输与物料搬运类职业", "Transportation and Material Moving", "16-运输与物料搬运类职业", 16),
        new SocNode("#17", null, "医疗支持类职业", "Healthcare Support", "17-医疗支持类职业", 17),
        new SocNode("#18", null, "保护性服务类职业", "Protective Service", "18-保护性服务类职业", 18),
        new SocNode("#19", null, "建筑与采掘类职业", "Construction and Extraction", "19-建筑与采掘类职业", 19),
        new SocNode("#20", null, "农业渔业与林业类职业", "Farming Fishing and Forestry", "20-农业渔业与林业类职业", 20),
        new SocNode("#21", null, "生产类职业", "Production", "21-生产类职业", 21),
        new SocNode("#22", null, "安装维护与修理类职业", "Installation Maintenance and Repair", "22-安装维护与修理类职业", 22),
        new SocNode("#23", null, "建筑与场地清洁维护类职业", "Building and Grounds Cleaning and Maintenance", "23-建筑与场地清洁维护类职业", 23),
        // 96 sub-group (level 2)
        new SocNode("01-01", "#01", "计算机职业", "Computer Occupations", "01-01-计算机职业", 1),
        new SocNode("01-02", "#01", "数学科学职业", "Mathematical Science Occupations", "01-02-数学科学职业", 2),
        new SocNode("02-01", "#02", "商业运营专员", "Business Operations Specialists", "02-01-商业运营专员", 1),
        new SocNode("02-02", "#02", "金融专员", "Financial Specialists", "02-02-金融专员", 2),
        new SocNode("03-01", "#03", "媒体与通信工作者", "Media and Communication Workers", "03-01-媒体与通信工作者", 1),
        new SocNode("03-02", "#03", "艺术与设计工作者", "Art and Design Workers", "03-02-艺术与设计工作者", 2),
        new SocNode("03-03", "#03", "娱乐演员运动员及相关工作者", "Entertainers Athletes and Related Workers", "03-03-娱乐演员运动员及相关工作者", 3),
        new SocNode("03-04", "#03", "媒体与通信设备工作者", "Media and Communication Equipment Workers", "03-04-媒体与通信设备工作者", 4),
        new SocNode("04-01", "#04", "信息与档案文员", "Information and Records Clerks", "04-01-信息与档案文员", 1),
        new SocNode("04-02", "#04", "秘书与行政助理", "Secretaries and Administrative Assistants", "04-02-秘书与行政助理", 2),
        new SocNode("04-03", "#04", "其他办公与行政支持工作者", "Other Office and Administrative Support Workers", "04-03-其他办公与行政支持工作者", 3),
        new SocNode("04-04", "#04", "财务文员", "Financial Clerks", "04-04-财务文员", 4),
        new SocNode("04-05", "#04", "物料记录调度派送与分发工人", "Material Recording Scheduling Dispatching and Distributing Workers", "04-05-物料记录调度派送与分发工人", 5),
        new SocNode("04-06", "#04", "办公与行政支持工人主管", "Office and Administrative Support Worker Supervisors", "04-06-办公与行政支持工人主管", 6),
        new SocNode("04-07", "#04", "通信设备操作员", "Communications Equipment Operators", "04-07-通信设备操作员", 7),
        new SocNode("05-01", "#05", "高等院校教师", "Postsecondary Teachers", "05-01-高等院校教师", 1),
        new SocNode("05-02", "#05", "其他教师与讲师", "Other Teachers and Instructors", "05-02-其他教师与讲师", 2),
        new SocNode("05-03", "#05", "其他教育教学与图书馆职业", "Other Educational Instruction and Library Occupations", "05-03-其他教育教学与图书馆职业", 3),
        new SocNode("05-04", "#05", "学前小学初中高中与特殊教育教师", "Preschool Elementary Middle Secondary and Special Education Teachers", "05-04-学前小学初中高中与特殊教育教师", 4),
        new SocNode("05-05", "#05", "图书馆员策展人与档案管理员", "Librarians Curators and Archivists", "05-05-图书馆员策展人与档案管理员", 5),
        new SocNode("06-01", "#06", "生命科学家", "Life Scientists", "06-01-生命科学家", 1),
        new SocNode("06-02", "#06", "物理科学家", "Physical Scientists", "06-02-物理科学家", 2),
        new SocNode("06-03", "#06", "社会科学家及相关工作者", "Social Scientists and Related Workers", "06-03-社会科学家及相关工作者", 3),
        new SocNode("06-04", "#06", "生命自然社会科学技术员", "Life Physical and Social Science Technicians", "06-04-生命自然社会科学技术员", 4),
        new SocNode("06-05", "#06", "职业健康与安全专员及技术员", "Occupational Health and Safety Specialists and Technicians", "06-05-职业健康与安全专员及技术员", 5),
        new SocNode("07-01", "#07", "运营专业管理人员", "Operations Specialties Managers", "07-01-运营专业管理人员", 1),
        new SocNode("07-02", "#07", "高级管理人员", "Top Executives", "07-02-高级管理人员", 2),
        new SocNode("07-03", "#07", "广告营销促销公关与销售管理人员", "Advertising Marketing Promotions Public Relations and Sales Managers", "07-03-广告营销促销公关与销售管理人员", 3),
        new SocNode("07-04", "#07", "其他管理职业", "Other Management Occupations", "07-04-其他管理职业", 4),
        new SocNode("08-01", "#08", "律师法官及相关工作者", "Lawyers Judges and Related Workers", "08-01-律师法官及相关工作者", 1),
        new SocNode("08-02", "#08", "法律支持工作者", "Legal Support Workers", "08-02-法律支持工作者", 2),
        new SocNode("09-01", "#09", "销售代表(服务业)", "Sales Representatives Services", "09-01-销售代表(服务业)", 1),
        new SocNode("09-02", "#09", "销售代表(批发与制造业)", "Sales Representatives Wholesale and Manufacturing", "09-02-销售代表(批发与制造业)", 2),
        new SocNode("09-03", "#09", "销售工人主管", "First-Line Supervisors of Sales Workers", "09-03-销售工人主管", 3),
        new SocNode("09-04", "#09", "其他销售及相关工作者", "Other Sales and Related Workers", "09-04-其他销售及相关工作者", 4),
        new SocNode("09-05", "#09", "零售销售工人", "Retail Sales Workers", "09-05-零售销售工人", 5),
        new SocNode("10-01", "#10", "工程师", "Engineers", "10-01-工程师", 1),
        new SocNode("10-02", "#10", "绘图员工程技术员与测绘技术员", "Drafters Engineering Technicians and Mapping Technicians", "10-02-绘图员工程技术员与测绘技术员", 2),
        new SocNode("10-03", "#10", "建筑师测量师与制图师", "Architects Surveyors and Cartographers", "10-03-建筑师测量师与制图师", 3),
        new SocNode("11-01", "#11", "医疗诊断或治疗从业者", "Healthcare Diagnosing or Treating Practitioners", "11-01-医疗诊断或治疗从业者", 1),
        new SocNode("11-02", "#11", "卫生技术人员与技术员", "Health Technologists and Technicians", "11-02-卫生技术人员与技术员", 2),
        new SocNode("11-03", "#11", "其他医疗从业者与技术职业", "Other Healthcare Practitioners and Technical Occupations", "11-03-其他医疗从业者与技术职业", 3),
        new SocNode("12-01", "#12", "其他个人护理与服务工作者", "Other Personal Care and Service Workers", "12-01-其他个人护理与服务工作者", 1),
        new SocNode("12-02", "#12", "导游与旅游向导", "Tour and Travel Guides", "12-02-导游与旅游向导", 2),
        new SocNode("12-03", "#12", "个人护理与服务工人主管", "First-Line Supervisors of Personal Care and Service Workers", "12-03-个人护理与服务工人主管", 3),
        new SocNode("12-04", "#12", "娱乐服务员及相关工作者", "Entertainment Attendants and Related Workers", "12-04-娱乐服务员及相关工作者", 4),
        new SocNode("12-05", "#12", "行李员门童与礼宾员", "Baggage Porters Bellhops and Concierges", "12-05-行李员门童与礼宾员", 5),
        new SocNode("12-06", "#12", "动物护理与服务工人", "Animal Care and Service Workers", "12-06-动物护理与服务工人", 6),
        new SocNode("12-07", "#12", "个人形象工作者", "Personal Appearance Workers", "12-07-个人形象工作者", 7),
        new SocNode("12-08", "#12", "殡葬服务工人", "Funeral Service Workers", "12-08-殡葬服务工人", 8),
        new SocNode("13-01", "#13", "咨询师社工及社区社会服务专员", "Counselors Social Workers and Community Service Specialists", "13-01-咨询师社工及社区社会服务专员", 1),
        new SocNode("13-02", "#13", "宗教工作者", "Religious Workers", "13-02-宗教工作者", 2),
        new SocNode("14-01", "#14", "军官特种与战术作战指挥官", "Military Officer Special and Tactical Operations Leaders", "14-01-军官特种与战术作战指挥官", 1),
        new SocNode("14-02", "#14", "军事士兵战术作战与空中武器专家及机组成员", "Military Enlisted Tactical Operations and Air Weapons Specialists and Crew Members", "14-02-军事士兵战术作战与空中武器专家及机组成员", 2),
        new SocNode("14-03", "#14", "一线士兵军事主管", "First-Line Enlisted Military Supervisors", "14-03-一线士兵军事主管", 3),
        new SocNode("15-01", "#15", "厨师与食品加工工人", "Chefs and Food Preparation Workers", "15-01-厨师与食品加工工人", 1),
        new SocNode("15-02", "#15", "食品准备与餐饮服务工人主管", "First-Line Supervisors of Food Preparation and Serving Workers", "15-02-食品准备与餐饮服务工人主管", 2),
        new SocNode("15-03", "#15", "餐饮服务工人", "Food and Beverage Serving Workers", "15-03-餐饮服务工人", 3),
        new SocNode("15-04", "#15", "其他食品准备与餐饮服务相关工作者", "Other Food Preparation and Serving Related Workers", "15-04-其他食品准备与餐饮服务相关工作者", 4),
        new SocNode("16-01", "#16", "其他运输工作者", "Other Transportation Workers", "16-01-其他运输工作者", 1),
        new SocNode("16-02", "#16", "机动车驾驶员", "Motor Vehicle Operators", "16-02-机动车驾驶员", 2),
        new SocNode("16-03", "#16", "运输与物料搬运工人主管", "First-Line Supervisors of Transportation and Material Moving Workers", "16-03-运输与物料搬运工人主管", 3),
        new SocNode("16-04", "#16", "航空运输工人", "Air Transportation Workers", "16-04-航空运输工人", 4),
        new SocNode("16-05", "#16", "铁路运输工人", "Rail Transportation Workers", "16-05-铁路运输工人", 5),
        new SocNode("16-06", "#16", "水路运输工人", "Water Transportation Workers", "16-06-水路运输工人", 6),
        new SocNode("16-07", "#16", "物料搬运工人", "Material Moving Workers", "16-07-物料搬运工人", 7),
        new SocNode("17-01", "#17", "家庭健康与个人护理助手及护理助理", "Home Health and Personal Care Aides and Nursing Assistants", "17-01-家庭健康与个人护理助手及护理助理", 1),
        new SocNode("17-02", "#17", "其他医疗支持职业", "Other Healthcare Support Occupations", "17-02-其他医疗支持职业", 2),
        new SocNode("17-03", "#17", "职业治疗与物理治疗助手及助理", "Occupational Therapy and Physical Therapist Assistants and Aides", "17-03-职业治疗与物理治疗助手及助理", 3),
        new SocNode("18-01", "#18", "执法工作者", "Law Enforcement Workers", "18-01-执法工作者", 1),
        new SocNode("18-02", "#18", "保护性服务工人主管", "First-Line Supervisors of Protective Service Workers", "18-02-保护性服务工人主管", 2),
        new SocNode("18-03", "#18", "其他保护性服务工作者", "Other Protective Service Workers", "18-03-其他保护性服务工作者", 3),
        new SocNode("18-04", "#18", "消防与防火工作者", "Firefighting and Fire Prevention Workers", "18-04-消防与防火工作者", 4),
        new SocNode("19-01", "#19", "建筑与采掘工人主管", "First-Line Supervisors of Construction and Extraction Workers", "19-01-建筑与采掘工人主管", 1),
        new SocNode("19-02", "#19", "其他建筑及相关工人", "Other Construction and Related Workers", "19-02-其他建筑及相关工人", 2),
        new SocNode("19-03", "#19", "建筑行业工人", "Construction Trades Workers", "19-03-建筑行业工人", 3),
        new SocNode("19-04", "#19", "采掘工人", "Extraction Workers", "19-04-采掘工人", 4),
        new SocNode("20-01", "#20", "农业工人", "Agricultural Workers", "20-01-农业工人", 1),
        new SocNode("20-02", "#20", "农业渔业与林业工人主管", "First-Line Supervisors of Farming Fishing and Forestry Workers", "20-02-农业渔业与林业工人主管", 2),
        new SocNode("20-03", "#20", "渔业与狩猎工人", "Fishing and Hunting Workers", "20-03-渔业与狩猎工人", 3),
        new SocNode("20-04", "#20", "森林保护与伐木工人", "Forest and Conservation Workers and Logging Workers", "20-04-森林保护与伐木工人", 4),
        new SocNode("21-01", "#21", "其他生产职业", "Other Production Occupations", "21-01-其他生产职业", 1),
        new SocNode("21-02", "#21", "生产工人主管", "First-Line Supervisors of Production Workers", "21-02-生产工人主管", 2),
        new SocNode("21-03", "#21", "纺织服装与家居工人", "Textile Apparel and Furnishings Workers", "21-03-纺织服装与家居工人", 3),
        new SocNode("21-04", "#21", "装配工与制造工", "Assemblers and Fabricators", "21-04-装配工与制造工", 4),
        new SocNode("21-05", "#21", "印刷工人", "Printing Workers", "21-05-印刷工人", 5),
        new SocNode("21-06", "#21", "金属工与塑料工", "Metal and Plastic Workers", "21-06-金属工与塑料工", 6),
        new SocNode("21-07", "#21", "工厂与系统操作员", "Plant and System Operators", "21-07-工厂与系统操作员", 7),
        new SocNode("21-08", "#21", "食品加工工人", "Food Processing Workers", "21-08-食品加工工人", 8),
        new SocNode("22-01", "#22", "车辆与移动设备维修技师与安装工", "Vehicle and Mobile Equipment Mechanics Installers and Repairers", "22-01-车辆与移动设备维修技师与安装工", 1),
        new SocNode("22-02", "#22", "其他安装维护与修理职业", "Other Installation Maintenance and Repair Occupations", "22-02-其他安装维护与修理职业", 2),
        new SocNode("22-03", "#22", "电气与电子设备维修安装工", "Electrical and Electronic Equipment Mechanics Installers and Repairers", "22-03-电气与电子设备维修安装工", 3),
        new SocNode("22-04", "#22", "安装维护与修理工人主管", "First-Line Supervisors of Installation Maintenance and Repair Workers", "22-04-安装维护与修理工人主管", 4),
        new SocNode("23-01", "#23", "建筑与场地清洁维护工人主管", "First-Line Supervisors of Building and Grounds Cleaning and Maintenance Workers", "23-01-建筑与场地清洁维护工人主管", 1),
        new SocNode("23-02", "#23", "场地维护工人", "Grounds Maintenance Workers", "23-02-场地维护工人", 2),
        new SocNode("23-03", "#23", "建筑清洁与害虫防治工人", "Building Cleaning and Pest Control Workers", "23-03-建筑清洁与害虫防治工人", 3)
    );

    /** S18: USAGE 节点（基于 skillsmp.com/zh/categories 抓取） */
    private static final List<UsageNode> USAGE_TAXONOMY = List.of(
        // 12 个一级
        new UsageNode("PURPOSE-TOOL", null, "工具", "Tools", "PURPOSE-TOOL-工具", 1),
        new UsageNode("PURPOSE-BIZ", null, "商业", "Business", "PURPOSE-BIZ-商业", 2),
        new UsageNode("PURPOSE-DEV", null, "开发", "Development", "PURPOSE-DEV-开发", 3),
        new UsageNode("PURPOSE-QASEC", null, "测试与安全", "Testing and Security", "PURPOSE-QASEC-测试与安全", 4),
        new UsageNode("PURPOSE-AI", null, "数据与AI", "Data and AI", "PURPOSE-AI-数据与AI", 5),
        new UsageNode("PURPOSE-DEVOPS", null, "DevOps", "DevOps", "PURPOSE-DEVOPS-DevOps", 6),
        new UsageNode("PURPOSE-DOC", null, "文档", "Documentation", "PURPOSE-DOC-文档", 7),
        new UsageNode("PURPOSE-MEDIA", null, "内容与媒体", "Content and Media", "PURPOSE-MEDIA-内容与媒体", 8),
        new UsageNode("PURPOSE-RESEARCH", null, "研究", "Research", "PURPOSE-RESEARCH-研究", 9),
        new UsageNode("PURPOSE-LIFE", null, "生活方式", "Lifestyle", "PURPOSE-LIFE-生活方式", 10),
        new UsageNode("PURPOSE-DB", null, "数据库", "Database", "PURPOSE-DB-数据库", 11),
        new UsageNode("PURPOSE-BLOCKCHAIN", null, "区块链", "Blockchain", "PURPOSE-BLOCKCHAIN-区块链", 12),
        // 70 sub-group
        new UsageNode("PURPOSE-TOOL-DEBUG", "PURPOSE-TOOL", "调试工具", "Debugging Tools", "PURPOSE-TOOL-DEBUG-调试工具", 1),
        new UsageNode("PURPOSE-TOOL-SYSADMIN", "PURPOSE-TOOL", "系统管理", "System Administration", "PURPOSE-TOOL-SYSADMIN-系统管理", 2),
        new UsageNode("PURPOSE-TOOL-PRODUCTIVITY", "PURPOSE-TOOL", "生产力工具", "Productivity Tools", "PURPOSE-TOOL-PRODUCTIVITY-生产力工具", 3),
        new UsageNode("PURPOSE-TOOL-AUTOMATION", "PURPOSE-TOOL", "自动化工具", "Automation Tools", "PURPOSE-TOOL-AUTOMATION-自动化工具", 4),
        new UsageNode("PURPOSE-TOOL-IDE", "PURPOSE-TOOL", "IDE 插件", "IDE Plugins", "PURPOSE-TOOL-IDE-IDE插件", 5),
        new UsageNode("PURPOSE-TOOL-CLI", "PURPOSE-TOOL", "命令行工具", "Command Line Tools", "PURPOSE-TOOL-CLI-命令行工具", 6),
        new UsageNode("PURPOSE-TOOL-DNS", "PURPOSE-TOOL", "域名与 DNS 工具", "Domain and DNS Tools", "PURPOSE-TOOL-DNS-域名与DNS工具", 7),
        new UsageNode("PURPOSE-BIZ-MARKETING", "PURPOSE-BIZ", "销售与营销", "Sales and Marketing", "PURPOSE-BIZ-MARKETING-销售与营销", 1),
        new UsageNode("PURPOSE-BIZ-PM", "PURPOSE-BIZ", "项目管理", "Project Management", "PURPOSE-BIZ-PM-项目管理", 2),
        new UsageNode("PURPOSE-BIZ-FINANCE", "PURPOSE-BIZ", "金融与投资", "Finance and Investment", "PURPOSE-BIZ-FINANCE-金融与投资", 3),
        new UsageNode("PURPOSE-BIZ-REALESTATE", "PURPOSE-BIZ", "房地产与法律", "Real Estate and Legal", "PURPOSE-BIZ-REALESTATE-房地产与法律", 4),
        new UsageNode("PURPOSE-BIZ-FITNESS", "PURPOSE-BIZ", "健康健身", "Health and Fitness", "PURPOSE-BIZ-FITNESS-健康健身", 5),
        new UsageNode("PURPOSE-BIZ-PAYMENT", "PURPOSE-BIZ", "支付", "Payment", "PURPOSE-BIZ-PAYMENT-支付", 6),
        new UsageNode("PURPOSE-BIZ-ECOMMERCE", "PURPOSE-BIZ", "电子商务", "E-commerce", "PURPOSE-BIZ-ECOMMERCE-电子商务", 7),
        new UsageNode("PURPOSE-BIZ-APPS", "PURPOSE-BIZ", "商业应用", "Business Applications", "PURPOSE-BIZ-APPS-商业应用", 8),
        new UsageNode("PURPOSE-DEV-ARCH", "PURPOSE-DEV", "架构模式", "Architecture Patterns", "PURPOSE-DEV-ARCH-架构模式", 1),
        new UsageNode("PURPOSE-DEV-BACKEND", "PURPOSE-DEV", "后端开发", "Backend Development", "PURPOSE-DEV-BACKEND-后端开发", 2),
        new UsageNode("PURPOSE-DEV-FRONTEND", "PURPOSE-DEV", "前端开发", "Frontend Development", "PURPOSE-DEV-FRONTEND-前端开发", 3),
        new UsageNode("PURPOSE-DEV-GAME", "PURPOSE-DEV", "游戏开发", "Game Development", "PURPOSE-DEV-GAME-游戏开发", 4),
        new UsageNode("PURPOSE-DEV-MOBILE", "PURPOSE-DEV", "移动开发", "Mobile Development", "PURPOSE-DEV-MOBILE-移动开发", 5),
        new UsageNode("PURPOSE-DEV-SCRIPT", "PURPOSE-DEV", "脚本编程", "Scripting", "PURPOSE-DEV-SCRIPT-脚本编程", 6),
        new UsageNode("PURPOSE-DEV-CMS", "PURPOSE-DEV", "CMS 与平台开发", "CMS and Platform Development", "PURPOSE-DEV-CMS-CMS与平台开发", 7),
        new UsageNode("PURPOSE-DEV-FULLSTACK", "PURPOSE-DEV", "全栈开发", "Full Stack Development", "PURPOSE-DEV-FULLSTACK-全栈开发", 8),
        new UsageNode("PURPOSE-DEV-PACKAGE", "PURPOSE-DEV", "包管理与发布", "Package Management and Publishing", "PURPOSE-DEV-PACKAGE-包管理与发布", 9),
        new UsageNode("PURPOSE-DEV-FRAMEWORK", "PURPOSE-DEV", "框架内核开发", "Framework and Kernel Development", "PURPOSE-DEV-FRAMEWORK-框架内核开发", 10),
        new UsageNode("PURPOSE-DEV-ECOMMERCE", "PURPOSE-DEV", "电商开发", "E-commerce Development", "PURPOSE-DEV-ECOMMERCE-电商开发", 11),
        new UsageNode("PURPOSE-QASEC-QUALITY", "PURPOSE-QASEC", "代码质量", "Code Quality", "PURPOSE-QASEC-QUALITY-代码质量", 1),
        new UsageNode("PURPOSE-QASEC-TESTING", "PURPOSE-QASEC", "测试", "Testing", "PURPOSE-QASEC-TESTING-测试", 2),
        new UsageNode("PURPOSE-QASEC-SECURITY", "PURPOSE-QASEC", "安全", "Security", "PURPOSE-QASEC-SECURITY-安全", 3),
        new UsageNode("PURPOSE-AI-LLM", "PURPOSE-AI", "LLM 与 AI", "LLM and AI", "PURPOSE-AI-LLM-LLM与AI", 1),
        new UsageNode("PURPOSE-AI-ML", "PURPOSE-AI", "机器学习", "Machine Learning", "PURPOSE-AI-ML-机器学习", 2),
        new UsageNode("PURPOSE-AI-DATAENG", "PURPOSE-AI", "数据工程", "Data Engineering", "PURPOSE-AI-DATAENG-数据工程", 3),
        new UsageNode("PURPOSE-AI-DATAANALYSIS", "PURPOSE-AI", "数据分析", "Data Analysis", "PURPOSE-AI-DATAANALYSIS-数据分析", 4),
        new UsageNode("PURPOSE-DEVOPS-GIT", "PURPOSE-DEVOPS", "Git 工作流", "Git Workflow", "PURPOSE-DEVOPS-GIT-Git工作流", 1),
        new UsageNode("PURPOSE-DEVOPS-CICD", "PURPOSE-DEVOPS", "CI/CD", "CI/CD", "PURPOSE-DEVOPS-CICD-CICD", 2),
        new UsageNode("PURPOSE-DEVOPS-CLOUD", "PURPOSE-DEVOPS", "云平台", "Cloud Platforms", "PURPOSE-DEVOPS-CLOUD-云平台", 3),
        new UsageNode("PURPOSE-DEVOPS-CONTAINER", "PURPOSE-DEVOPS", "容器", "Containers", "PURPOSE-DEVOPS-CONTAINER-容器", 4),
        new UsageNode("PURPOSE-DEVOPS-MONITORING", "PURPOSE-DEVOPS", "监控", "Monitoring", "PURPOSE-DEVOPS-MONITORING-监控", 5),
        new UsageNode("PURPOSE-DOC-KB", "PURPOSE-DOC", "知识库", "Knowledge Base", "PURPOSE-DOC-KB-知识库", 1),
        new UsageNode("PURPOSE-DOC-TECH", "PURPOSE-DOC", "技术文档", "Technical Documentation", "PURPOSE-DOC-TECH-技术文档", 2),
        new UsageNode("PURPOSE-DOC-EDUCATION", "PURPOSE-DOC", "教育", "Education", "PURPOSE-DOC-EDUCATION-教育", 3),
        new UsageNode("PURPOSE-MEDIA-DOC", "PURPOSE-MEDIA", "文档处理", "Document Processing", "PURPOSE-MEDIA-DOC-文档处理", 1),
        new UsageNode("PURPOSE-MEDIA-CONTENT", "PURPOSE-MEDIA", "内容创作", "Content Creation", "PURPOSE-MEDIA-CONTENT-内容创作", 2),
        new UsageNode("PURPOSE-MEDIA-DESIGN", "PURPOSE-MEDIA", "设计", "Design", "PURPOSE-MEDIA-DESIGN-设计", 3),
        new UsageNode("PURPOSE-MEDIA-MEDIA", "PURPOSE-MEDIA", "媒体处理", "Media Processing", "PURPOSE-MEDIA-MEDIA-媒体处理", 4),
        new UsageNode("PURPOSE-RESEARCH-ACADEMIC", "PURPOSE-RESEARCH", "学术研究", "Academic Research", "PURPOSE-RESEARCH-ACADEMIC-学术研究", 1),
        new UsageNode("PURPOSE-RESEARCH-BIO", "PURPOSE-RESEARCH", "生物信息学", "Bioinformatics", "PURPOSE-RESEARCH-BIO-生物信息学", 2),
        new UsageNode("PURPOSE-RESEARCH-LAB", "PURPOSE-RESEARCH", "实验室工具", "Laboratory Tools", "PURPOSE-RESEARCH-LAB-实验室工具", 3),
        new UsageNode("PURPOSE-RESEARCH-CHEM", "PURPOSE-RESEARCH", "计算化学", "Computational Chemistry", "PURPOSE-RESEARCH-CHEM-计算化学", 4),
        new UsageNode("PURPOSE-RESEARCH-COMPUTE", "PURPOSE-RESEARCH", "科学计算", "Scientific Computing", "PURPOSE-RESEARCH-COMPUTE-科学计算", 5),
        new UsageNode("PURPOSE-RESEARCH-ASTRO", "PURPOSE-RESEARCH", "天文物理", "Astronomy and Physics", "PURPOSE-RESEARCH-ASTRO-天文物理", 6),
        new UsageNode("PURPOSE-LIFE-LITERATURE", "PURPOSE-LIFE", "文学与写作", "Literature and Writing", "PURPOSE-LIFE-LITERATURE-文学与写作", 1),
        new UsageNode("PURPOSE-LIFE-PHILOSOPHY", "PURPOSE-LIFE", "哲学与伦理", "Philosophy and Ethics", "PURPOSE-LIFE-PHILOSOPHY-哲学与伦理", 2),
        new UsageNode("PURPOSE-LIFE-WELLNESS", "PURPOSE-LIFE", "健康养生", "Wellness", "PURPOSE-LIFE-WELLNESS-健康养生", 3),
        new UsageNode("PURPOSE-LIFE-ARTS", "PURPOSE-LIFE", "艺术与手工", "Arts and Crafts", "PURPOSE-LIFE-ARTS-艺术与手工", 4),
        new UsageNode("PURPOSE-LIFE-DIVINATION", "PURPOSE-LIFE", "占卜与玄学", "Divination and Mysticism", "PURPOSE-LIFE-DIVINATION-占卜与玄学", 5),
        new UsageNode("PURPOSE-LIFE-CULINARY", "PURPOSE-LIFE", "烹饪艺术", "Culinary Arts", "PURPOSE-LIFE-CULINARY-烹饪艺术", 6),
        new UsageNode("PURPOSE-DB-TOOLS", "PURPOSE-DB", "数据库工具", "Database Tools", "PURPOSE-DB-TOOLS-数据库工具", 1),
        new UsageNode("PURPOSE-DB-SQL", "PURPOSE-DB", "SQL 数据库", "SQL Databases", "PURPOSE-DB-SQL-SQL数据库", 2),
        new UsageNode("PURPOSE-DB-NOSQL", "PURPOSE-DB", "NoSQL 数据库", "NoSQL Databases", "PURPOSE-DB-NOSQL-NoSQL数据库", 3),
        new UsageNode("PURPOSE-BLOCKCHAIN-CONTRACT", "PURPOSE-BLOCKCHAIN", "智能合约", "Smart Contracts", "PURPOSE-BLOCKCHAIN-CONTRACT-智能合约", 1),
        new UsageNode("PURPOSE-BLOCKCHAIN-WEB3", "PURPOSE-BLOCKCHAIN", "Web3 工具", "Web3 Tools", "PURPOSE-BLOCKCHAIN-WEB3-Web3工具", 2),
        new UsageNode("PURPOSE-BLOCKCHAIN-DEFI", "PURPOSE-BLOCKCHAIN", "DeFi", "DeFi", "PURPOSE-BLOCKCHAIN-DEFI-DeFi", 3)
    );

    /** S18/S20: skill name / plugin slug 关键词 → USAGE code 启发式映射（已抽到 CategoryUtil） */
    private static String guessUsageCode(String pluginSlug, String name) {
        return CategoryUtil.guessUsageCode(pluginSlug, name);
    }

    /** S04: 旧 10 domain → 新 SOC code 映射 */
    private static final Map<String, String> OLD_TO_NEW_CODE = Map.of(
        "web", "01-01",
        "testing", "01-01",
        "devops", "01-01",
        "docs", "03-01",
        "code-quality", "01-01",
        "design", "03-02",
        "productivity", "01-01",
        "database", "01-01",
        "ai-ml", "01-02",
        "data", "01-02"
    );

    private static final Map<String, String> OLD_TO_NEW_SLUG = Map.of(
        "web", "01-01-计算机职业",
        "testing", "01-01-计算机职业",
        "devops", "01-01-计算机职业",
        "docs", "03-01-媒体与通信工作者",
        "code-quality", "01-01-计算机职业",
        "design", "03-02-艺术与设计工作者",
        "productivity", "01-01-计算机职业",
        "database", "01-01-计算机职业",
        "ai-ml", "01-02-数学科学职业",
        "data", "01-02-数学科学职业"
    );

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    @Transactional
    public void seed() {
        if (!seedProperties.getSeed().isEnabled()) {
            log.info("[seed] disabled by config, skip");
            return;
        }
        try {
            seedSocTaxonomy();
            seedUsageTaxonomy();
            seedSlugRedirects();
            seedDefaultUsers();
            seedSkills();
        } catch (Exception e) {
            log.error("[seed] failed", e);
        }
    }

    /** S04: 装 SOC 23 一级 + 96 sub-group。幂等。 */
    private void seedSocTaxonomy() {
        Long socTopCount = categoryMapper.selectCount(new QueryWrapper<Category>().likeRight("code", "#"));
        if (socTopCount != null && socTopCount >= 23) {
            log.info("[seed] SOC taxonomy already seeded ({} top), skip", socTopCount);
            return;
        }
        skillMapper.update(null, new UpdateWrapper<Skill>().set("category_id", null));
        categoryMapper.delete(new QueryWrapper<Category>());
        log.info("[seed] cleared legacy categories and unlinked skills");

        LocalDateTime now = LocalDateTime.now();
        for (SocNode s : SOC_TAXONOMY) {
            if (s.parentCode() == null) {
                categoryMapper.insert(buildCategory(s, null, now));
            }
        }
        Map<String, Long> codeToId = new HashMap<>();
        for (Category c : categoryMapper.selectList(null)) {
            if (c.getCode() != null) codeToId.put(c.getCode(), c.getId());
        }
        for (SocNode s : SOC_TAXONOMY) {
            if (s.parentCode() != null) {
                Long parentId = codeToId.get(s.parentCode());
                categoryMapper.insert(buildCategory(s, parentId, now));
            }
        }
        long total = categoryMapper.selectCount(null);
        log.info("[seed] SOC taxonomy seeded: 23 top + 96 sub = {} categories", total);
    }

    private Category buildCategory(SocNode s, Long parentId, LocalDateTime now) {
        Category c = new Category();
        c.setType("SOC");
        c.setCode(s.code());
        c.setParentId(parentId);
        c.setName(s.nameZh());
        c.setDescription(s.nameEn());
        c.setSlug(s.slug());
        c.setSortOrder(s.sortOrder());
        c.setSkillCount(0);
        c.setCreateTime(now);
        c.setUpdateTime(now);
        return c;
    }

    /** S18: 装 USAGE 12 一级 + 70 sub-group = 82。幂等。 */
    private void seedUsageTaxonomy() {
        Long usageTopCount = categoryMapper.selectCount(new QueryWrapper<Category>().likeRight("code", "PURPOSE-"));
        if (usageTopCount != null && usageTopCount >= 12) {
            log.info("[seed] USAGE taxonomy already seeded ({} top), skip", usageTopCount);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (UsageNode u : USAGE_TAXONOMY) {
            if (u.parentCode() == null) {
                categoryMapper.insert(buildUsageCategory(u, null, now));
            }
        }
        Map<String, Long> codeToId = new HashMap<>();
        for (Category c : categoryMapper.selectList(new QueryWrapper<Category>().likeRight("code", "PURPOSE-"))) {
            if (c.getCode() != null) codeToId.put(c.getCode(), c.getId());
        }
        for (UsageNode u : USAGE_TAXONOMY) {
            if (u.parentCode() != null) {
                Long parentId = codeToId.get(u.parentCode());
                categoryMapper.insert(buildUsageCategory(u, parentId, now));
            }
        }
        long total = categoryMapper.selectCount(new QueryWrapper<Category>().likeRight("code", "PURPOSE-"));
        log.info("[seed] USAGE taxonomy seeded: 12 top + 70 sub = {} categories", total);
    }

    private Category buildUsageCategory(UsageNode u, Long parentId, LocalDateTime now) {
        Category c = new Category();
        c.setType("USAGE");
        c.setCode(u.code());
        c.setParentId(parentId);
        c.setName(u.nameZh());
        c.setDescription(u.nameEn());
        c.setSlug(u.slug());
        c.setSortOrder(u.sortOrder());
        c.setSkillCount(0);
        c.setCreateTime(now);
        c.setUpdateTime(now);
        return c;
    }

    /** S04: 装旧 slug → 新 slug 重定向。幂等。 */
    private void seedSlugRedirects() {
        try {
            jdbc.execute("DELETE FROM category_slug_redirect");
        } catch (Exception e) {
            log.warn("[seed] clear redirects failed: {}", e.getMessage());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, String> e : OLD_TO_NEW_SLUG.entrySet()) {
            jdbc.update("INSERT INTO category_slug_redirect (old_slug, new_slug, create_time) VALUES (?, ?, ?)",
                    e.getKey(), e.getValue(), java.sql.Timestamp.valueOf(now));
        }
        log.info("[seed] slug redirects seeded: {}", OLD_TO_NEW_SLUG.size());
    }

    private void seedDefaultUsers() {
        if (userMapper.selectCount(null) > 0) {
            log.info("[seed] users already exist, skip");
            return;
        }
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        LocalDateTime now = LocalDateTime.now();
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(enc.encode("admin123"));
        admin.setEmail("admin@skillsmap.local");
        admin.setDisplayName("Administrator");
        admin.setAvatar("ADMIN");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setCreateTime(now);
        admin.setUpdateTime(now);

        User user = new User();
        user.setUsername("user");
        user.setPassword(enc.encode("user123"));
        user.setEmail("user@skillsmap.local");
        user.setDisplayName("Demo User");
        user.setAvatar("USER");
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(now);
        user.setUpdateTime(now);

        userMapper.insert(admin);
        userMapper.insert(user);
        log.info("[seed] inserted default users: admin/admin123, user/user123");
    }

    private void seedSkills() {
        Path classpathRoot = null;
        if (skillMapper.selectCount(null) > 0) {
            log.info("[seed] skills already exist, skip bulk import");
            // S21: 但仍要扫 classpath:skills/，保证 bundled skill（skills-manager）能被补齐
            classpathRoot = resolveClasspathRoot();
            int bundled = scanClasspathSkills();
            if (bundled > 0) {
                refreshCategoryCount();
                log.info("[seed] total {} bundled skills imported (retroactive)", bundled);
            }
            // S22: 无论是否新装，都把 classpath 包物化到 storage root（idempotent）
            materializeBundledToStorage(skillStorageService, classpathRoot);
            return;
        }
        String userPath = seedProperties.getSeed().getLocalSkillsPath();
        String pluginsPath = seedProperties.getSeed().getLocalPluginsPath();
        int total = 0;
        if (StrUtil.isNotBlank(userPath)) total += scanDir(Paths.get(userPath), "official", true);
        if (StrUtil.isNotBlank(pluginsPath)) total += scanPluginsDir(Paths.get(pluginsPath));
        // S21: 扫 classpath:skills/ —— 装应用自带的官方 skill（如 skills-manager）
        classpathRoot = resolveClasspathRoot();
        total += scanClasspathSkills();
        // S22: 把 classpath 包物化到 storage root
        materializeBundledToStorage(skillStorageService, classpathRoot);
        refreshCategoryCount();
        log.info("[seed] total {} skills imported", total);
    }

    /**
     * S22: 取 classpath:skills/ 根目录（jar 协议时拆到临时目录）。
     * 用于随后的物化步骤。
     */
    private Path resolveClasspathRoot() {
        log.info("[seed] resolveClasspathRoot: probing classpath");
        // 1. 优先从本类所在 jar 拆（绕过 Spring Boot 3.x nested: 协议坑）
        try {
            Path fromJar = extractClasspathSkillsFromCodeSource();
            if (fromJar != null) {
                log.info("[seed] resolveClasspathRoot: from jar = {}", fromJar);
                return fromJar;
            }
        } catch (Exception e) {
            log.warn("[seed] extractClasspathSkillsFromCodeSource threw: {}", e.getMessage());
        }
        // 2. 兜底：ClassLoader.getResource
        try {
            java.net.URL url = SkillSeedService.class.getClassLoader().getResource("skills");
            if (url == null) return null;
            if ("jar".equals(url.getProtocol())) {
                java.io.File tmp = java.io.File.createTempFile("skills-", "-mat");
                tmp.delete();
                tmp.mkdirs();
                Path root = tmp.toPath();
                try (java.util.stream.Stream<Path> walk = Files.walk(extractFromJar(url, root))) {
                    walk.forEach(p -> {});
                }
                return root;
            }
            return Paths.get(url.toURI());
        } catch (Exception e) {
            log.warn("[seed] resolveClasspathRoot fallback failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 直接从本类所在 jar 拆出 BOOT-INF/classes/skills/ 到临时目录。
     * 适配 Spring Boot 3.x LaunchedClassLoader 暴露的 nested: 协议。
     * 简化版：直接尝试常见 jar 路径（生产 Dockerfile COPY /app/app.jar）。
     */
    private Path extractClasspathSkillsFromCodeSource() {
        log.info("[seed] extractClasspathSkillsFromCodeSource: scanning known jar locations");
        // 1. 优先从 ProtectionDomain 拿（IDE / 普通 jar 包时）
        try {
            java.security.CodeSource cs = SkillSeedService.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null && "file".equals(cs.getLocation().getProtocol())) {
                java.io.File f = new java.io.File(cs.getLocation().toURI());
                if (f.isFile() && f.getName().endsWith(".jar")) {
                    Path r = unzipSkillsFromJar(f);
                    if (r != null) {
                        log.info("[seed] extracted classpath skills from {}", f.getAbsolutePath());
                        return r;
                    }
                } else if (f.isDirectory()) {
                    Path p = f.toPath().resolve("skills");
                    if (Files.isDirectory(p)) return p;
                }
            }
        } catch (Exception e) {
            log.warn("[seed] ProtectionDomain lookup failed: {}", e.getMessage());
        }
        // 2. 兜底：硬编码常见路径（Dockerfile COPY /app/app.jar）
        String[] fallbacks = {"/app/app.jar", "/opt/app.jar",
                System.getProperty("user.dir") + "/app.jar",
                new java.io.File(".").getAbsolutePath() + "/app.jar"};
        for (String p : fallbacks) {
            java.io.File f = new java.io.File(p);
            log.info("[seed] try candidate: {} (exists={})", p, f.isFile());
            if (f.isFile() && f.getName().endsWith(".jar")) {
                try {
                    Path r = unzipSkillsFromJar(f);
                    if (r != null) {
                        log.info("[seed] extracted classpath skills from fallback {}", f.getAbsolutePath());
                        return r;
                    }
                } catch (Exception e) {
                    log.warn("[seed] unzip {} failed: {}", p, e.getMessage());
                }
            }
        }
        log.warn("[seed] no jar candidate found, classpath skills unavailable");
        return null;
    }

    private Path unzipSkillsFromJar(java.io.File jarFile) throws java.io.IOException {
        if (!jarFile.isFile() || !jarFile.getName().endsWith(".jar")) return null;
        java.io.File tmp = java.io.File.createTempFile("skills-", "-src");
        tmp.delete();
        tmp.mkdirs();
        Path root = tmp.toPath();
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(jarFile)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                java.util.zip.ZipEntry e = en.nextElement();
                String name = e.getName();
                String rel;
                if (name.startsWith("BOOT-INF/classes/skills/")) {
                    rel = name.substring("BOOT-INF/classes/".length());
                } else if (name.startsWith("skills/")) {
                    rel = name;
                } else {
                    continue;
                }
                Path out = root.resolve(rel).normalize();
                if (!out.startsWith(root)) continue;
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (java.io.InputStream in = zf.getInputStream(e)) {
                        Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        Path skills = root.resolve("skills");
        return Files.isDirectory(skills) ? skills : null;
    }

    /**
     * S21: 扫描 classpath:skills/ 下的 skill 目录。
     * <p>每个子目录若有 SKILL.md 即视为一个 skill。
     * <p>来源标记为 {@code official-bundled} 以区别于用户/插件来源。
     * <p>用于把 skills-manager 等系统级 skill 随 jar 一同发布，确保 slug 可在 /api/skills 查到。
     * 修复：Spring Boot 3.x nested: 协议下走 extractClasspathSkillsFromCodeSource() 拆 jar。
     */
    private int scanClasspathSkills() {
        try {
            // 1. 优先从本类所在 jar 拆（绕开 nested: 协议坑）
            Path root = null;
            try {
                root = extractClasspathSkillsFromCodeSource();
            } catch (Exception e) {
                log.warn("[seed] scanClasspathSkills: extractFromCodeSource threw: {}", e.getMessage());
            }
            // 2. 兜底：ClassLoader.getResource
            if (root == null) {
                try {
                    java.net.URL url = SkillSeedService.class.getClassLoader().getResource("skills");
                    if (url == null) {
                        log.info("[seed] no classpath:skills/ found, skip");
                        return 0;
                    }
                    if ("jar".equals(url.getProtocol())) {
                        java.io.File tmp = java.io.File.createTempFile("skills-", "-cp");
                        tmp.delete();
                        tmp.mkdirs();
                        root = tmp.toPath();
                        try (java.util.stream.Stream<Path> walk = Files.walk(extractFromJar(url, root))) {
                            walk.forEach(p -> {});
                        }
                    } else {
                        root = Paths.get(url.toURI());
                    }
                } catch (Exception e) {
                    log.warn("[seed] scanClasspathSkills fallback failed: {}", e.getMessage());
                    return 0;
                }
            }
            if (root == null || !Files.isDirectory(root)) {
                log.warn("[seed] classpath:skills/ not a dir: {}", root);
                return 0;
            }
            int count = 0;
            try (Stream<Path> stream = Files.list(root)) {
                List<Path> dirs = stream.filter(Files::isDirectory).toList();
                for (Path dir : dirs) {
                    Path md = dir.resolve("SKILL.md");
                    if (Files.isRegularFile(md)) {
                        try {
                            if (importSkill(md, "official-bundled", true)) count++;
                        } catch (Exception e) {
                            log.warn("[seed] failed to import classpath skill {}: {}", md, e.getMessage());
                        }
                    }
                }
            }
            log.info("[seed] classpath:skills/ scanned, {} bundled skills imported", count);
            return count;
        } catch (Exception e) {
            log.warn("[seed] scanClasspathSkills failed: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * S22: 把 classpath:skills/ 下的所有 bundled skill 目录物化到 storage root，
     * 让 exportZip 优先走本地目录路径（包含 references/、scripts/ 等完整资源）。
     * <p>幂等：目标目录已存在且 SKILL.md 内容一致时跳过；否则覆盖。
     * <p>注意：seed 阶段 storage 还没起来？会的，@PostConstruct 早于 ApplicationReadyEvent。
     */
    private void materializeBundledToStorage(SkillStorageService storage, Path classpathRoot) {
        if (storage == null || classpathRoot == null || !Files.isDirectory(classpathRoot)) return;
        try (Stream<Path> stream = Files.list(classpathRoot)) {
            List<Path> dirs = stream.filter(Files::isDirectory).toList();
            int materialized = 0;
            for (Path dir : dirs) {
                Path md = dir.resolve("SKILL.md");
                if (!Files.isRegularFile(md)) continue;
                String name = dir.getFileName().toString();
                Path target = storage.skillDir(name);
                if (isAlreadyMaterialized(target, md)) {
                    log.debug("[seed] classpath skill already materialized: {}", name);
                    continue;
                }
                try {
                    copyDir(dir, target);
                    materialized++;
                    log.info("[seed] materialized bundled skill to storage: {} -> {}", name, target);
                } catch (Exception e) {
                    log.warn("[seed] materialize {} failed: {}", name, e.getMessage());
                }
            }
            if (materialized > 0) {
                log.info("[seed] materialized {} bundled skill(s) to storage root", materialized);
            }
        } catch (Exception e) {
            log.warn("[seed] materializeBundledToStorage failed: {}", e.getMessage());
        }
    }

    private boolean isAlreadyMaterialized(Path target, Path classpathMd) throws IOException {
        if (!Files.isDirectory(target)) return false;
        Path targetMd = target.resolve("SKILL.md");
        if (!Files.isRegularFile(targetMd)) return false;
        // 字节比对：相同则视作已物化
        return Files.mismatch(classpathMd, targetMd) == -1L;
    }

    private void copyDir(Path src, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (Stream<Path> walk = Files.walk(src)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                String rel = src.relativize(p).toString().replace('\\', '/');
                if (rel.isEmpty()) continue;
                Path t = dest.resolve(rel).normalize();
                if (!t.startsWith(dest.normalize())) {
                    throw new IOException("materialize: 越界路径 " + rel);
                }
                if (Files.isDirectory(p)) {
                    Files.createDirectories(t);
                } else {
                    Files.createDirectories(t.getParent());
                    Files.copy(p, t, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Path extractFromJar(java.net.URL jarUrl, Path dest) throws IOException {
        // jarUrl 形如 jar:file:/.../app.jar!/skills
        String full = jarUrl.getPath();
        int sep = full.indexOf("!");
        String jarFilePath = full.substring(0, sep);
        String entryPath = full.substring(sep + 1); // "/skills"
        if (jarFilePath.startsWith("file:")) {
            jarFilePath = jarFilePath.substring("file:".length());
        }
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFilePath)) {
            java.util.Enumeration<java.util.jar.JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                java.util.jar.JarEntry e = en.nextElement();
                String name = e.getName();
                if (!name.startsWith(entryPath)) continue;
                String rel = name.substring(entryPath.length());
                if (rel.startsWith("/")) rel = rel.substring(1);
                if (rel.isEmpty()) continue;
                Path target = dest.resolve(rel);
                if (e.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (var in = jar.getInputStream(e)) {
                        Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        return dest;
    }

    private int scanDir(Path root, String source, boolean featured) {
        if (!Files.isDirectory(root)) { log.warn("[seed] dir not found: {}", root); return 0; }
        int count = 0;
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> dirs = stream.filter(Files::isDirectory).toList();
            for (Path dir : dirs) {
                Path md = dir.resolve("SKILL.md");
                if (Files.isRegularFile(md)) {
                    try { if (importSkill(md, source, featured)) count++; }
                    catch (Exception e) { log.warn("[seed] failed to import {}: {}", md, e.getMessage()); }
                }
            }
        } catch (IOException e) { log.warn("[seed] list dir failed: {}", e.getMessage()); }
        return count;
    }

    private int scanPluginsDir(Path root) {
        if (!Files.isDirectory(root)) { log.warn("[seed] plugins dir not found: {}", root); return 0; }
        int count = 0;
        try (Stream<Path> plugins = Files.list(root)) {
            List<Path> pluginDirs = plugins.filter(Files::isDirectory).toList();
            for (Path pluginDir : pluginDirs) {
                Path pluginJson = pluginDir.resolve(".claude-plugin").resolve("plugin.json");
                Map<String, Object> pluginMeta = readJson(pluginJson);
                Path skillsRoot = pluginDir.resolve("skills");
                if (Files.isDirectory(skillsRoot)) {
                    try (Stream<Path> skillDirs = Files.list(skillsRoot)) {
                        List<Path> sdList = skillDirs.filter(Files::isDirectory).toList();
                        for (Path sd : sdList) {
                            Path md = sd.resolve("SKILL.md");
                            if (Files.isRegularFile(md)) {
                                try { if (importSkill(md, "official", false, pluginMeta, pluginDir.getFileName().toString())) count++; }
                                catch (Exception e) { log.warn("[seed] failed to import {}: {}", md, e.getMessage()); }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) { log.warn("[seed] scan plugins failed: {}", e.getMessage()); }
        return count;
    }

    private boolean importSkill(Path mdPath, String source, boolean featured) {
        return importSkill(mdPath, source, featured, null, null);
    }

    private boolean importSkill(Path mdPath, String source, boolean featured,
                                 Map<String, Object> pluginMeta, String pluginSlug) {
        try {
            MarkdownFrontmatterParser.Parsed parsed = MarkdownFrontmatterParser.parseFile(mdPath);
            Map<String, Object> fm = parsed.getFrontmatter();
            String name = (String) fm.get("name");
            if (StrUtil.isBlank(name)) { log.warn("[seed] no name in frontmatter, skip: {}", mdPath); return false; }
            if (skillMapper.selectCount(new QueryWrapper<Skill>().eq("name", name)) > 0) return false;

            Path manifest = mdPath.getParent().resolve("manifest.json");
            Map<String, Object> manifestData = readJson(manifest);

            Skill skill = new Skill();
            skill.setName(name);
            skill.setSlug(name);
            skill.setDisplayName(deriveDisplayName(name, fm));
            skill.setDescription(StrUtil.maxLength((String) fm.get("description"), 1024));
            skill.setBody(parsed.getBody());
            skill.setLicense((String) fm.get("license"));
            skill.setAllowedTools((String) fm.get("allowed-tools"));
            skill.setCompatibility((String) fm.get("compatibility"));
            skill.setMetadata(fm.containsKey("metadata") ? JSON.toJSONString(fm.get("metadata")) : null);
            skill.setVersion((String) (manifestData != null ? manifestData.get("version") : null));
            skill.setHomepage((String) (manifestData != null ? manifestData.get("homepage") : null));
            if (pluginMeta != null) {
                Object author = pluginMeta.get("author");
                if (author instanceof Map) {
                    Map<?, ?> am = (Map<?, ?>) author;
                    skill.setAuthorName((String) am.get("name"));
                    skill.setAuthorEmail((String) am.get("email"));
                }
            }
            // S04: 旧 pluginSlug / skill name 关键词 → SOC code → category_id
            String socCode = guessSocCode(pluginSlug, name);
            skill.setCategoryId(socCode == null ? null : categoryIdBySocCode(socCode));
            // S18: 启发式填 USAGE 维度
            String usageCode = guessUsageCode(pluginSlug, name);
            skill.setUsageCategoryId(categoryIdByUsageCode(usageCode));
            skill.setTags("[]");
            skill.setSource(source);
            skill.setInstallCommand("npx skills add " + (pluginSlug != null ? pluginSlug : "anthropics/skills") + "@" + name);
            skill.setStatus("published");
            skill.setFeatured(featured || isFeatured(name));
            skill.setRatingAvg(0.0);
            skill.setRatingCount(0);
            skill.setStars(0);
            skill.setInstalls(0);
            skill.setViews(0);
            skill.setIcon(guessIcon(name));
            skill.setCreateTime(LocalDateTime.now());
            skill.setUpdateTime(LocalDateTime.now());

            skillMapper.insert(skill);
            extractTags(skill, fm);
            return true;
        } catch (Exception e) {
            log.warn("[seed] importSkill failed: {} - {}", mdPath, e.getMessage());
            return false;
        }
    }

    private String deriveDisplayName(String name, Map<String, Object> fm) {
        Object dn = fm.get("display-name");
        if (dn != null) return dn.toString();
        String[] parts = name.split("-");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    /** S04: 按 skill name 推断 SOC code（替代旧的 guessCategoryId） */
    private String guessSocCode(String pluginSlug, String name) {
        String n = name.toLowerCase();
        if (n.contains("test") || n.contains("qa")) return "01-01";
        if (n.contains("design") || n.contains("ui") || n.contains("ux") || n.contains("art")
                || n.contains("canvas") || n.contains("theme") || n.contains("brand")) return "03-02";
        if (n.contains("doc") || n.contains("pdf") || n.contains("pptx") || n.contains("docx")
                || n.contains("readme") || n.contains("internal-comms") || n.contains("coauthoring")) return "03-01";
        if (n.contains("data") || n.contains("sql") || n.contains("db") || n.contains("math")
                || n.contains("algorithmic") || n.contains("xlsx")) return "01-02";
        if (n.contains("ai") || n.contains("ml") || n.contains("claude-api") || n.contains("gpt")
                || n.contains("llm") || n.contains("deep-research")) return "01-02";
        if (n.contains("video") || n.contains("gif") || n.contains("presentation")) return "03-04";
        return "01-01";
    }

    private Long categoryIdBySocCode(String socCode) {
        Category c = categoryMapper.selectOne(new QueryWrapper<Category>().eq("code", socCode).last("LIMIT 1"));
        return c == null ? null : c.getId();
    }

    private Long categoryIdByUsageCode(String usageCode) {
        return CategoryUtil.categoryIdByUsageCode(categoryMapper, usageCode);
    }

    private boolean isFeatured(String name) {
        List<String> featured = List.of("find-skills", "skill-creator", "claude-api", "ui-ux-pro-max",
                "code-review", "deep-research", "web-video-presentation");
        return featured.contains(name);
    }

    private String guessIcon(String name) {
        String n = name.toLowerCase();
        if (n.contains("test") || n.contains("qa")) return "TEST";
        if (n.contains("design") || n.contains("ui") || n.contains("ux")) return "DESIGN";
        if (n.contains("doc")) return "DOC";
        if (n.contains("data") || n.contains("sql") || n.contains("db")) return "DATA";
        if (n.contains("research") || n.contains("ai") || n.contains("ml")) return "AI";
        if (n.contains("review") || n.contains("simplify") || n.contains("code")) return "CODE";
        if (n.contains("video") || n.contains("presentation")) return "VIDEO";
        if (n.contains("find") || n.contains("skill")) return "FIND";
        if (n.contains("api") || n.contains("claude")) return "AI";
        if (n.contains("web") || n.contains("frontend")) return "WEB";
        if (n.contains("mcp") || n.contains("server")) return "PLUG";
        if (n.contains("hook")) return "HOOK";
        return "PKG";
    }

    private void extractTags(Skill skill, Map<String, Object> fm) {
        if (skill.getId() == null) return;
        Set<String> tagSet = new LinkedHashSet<>();
        Object meta = fm.get("metadata");
        if (meta instanceof Map) {
            Object ts = ((Map<?, ?>) meta).get("tags");
            if (ts instanceof List) { for (Object t : (List<?>) ts) tagSet.add(t.toString()); }
        }
        String desc = (String) fm.getOrDefault("description", "");
        String[] keywords = {"claude", "code", "review", "test", "design", "api", "web", "ai", "data", "devops", "video", "mcp"};
        for (String k : keywords) { if (desc.toLowerCase().contains(k)) tagSet.add(k); }
        if (tagSet.isEmpty()) return;
        List<String> tagNames = new ArrayList<>(tagSet);
        skill.setTags(JSON.toJSONString(tagNames));
        for (String tn : tagNames) {
            Tag tag = findOrCreateTag(tn);
            if (tag != null) {
                try {
                    com.meiya.skillsmap.entity.SkillTag st = new com.meiya.skillsmap.entity.SkillTag();
                    st.setSkillId(skill.getId());
                    st.setTagId(tag.getId());
                    skillTagMapper.insert(st);
                } catch (Exception ignored) {}
            }
        }
    }

    private Tag findOrCreateTag(String name) {
        String slug = TagServiceImpl.slugify(name);
        if (slug.isEmpty()) return null;
        Tag existing = tagMapper.selectOne(new QueryWrapper<Tag>().eq("slug", slug));
        if (existing != null) return existing;
        Tag t = new Tag();
        t.setName(name);
        t.setSlug(slug);
        t.setSkillCount(0);
        t.setCreateTime(LocalDateTime.now());
        tagMapper.insert(t);
        return t;
    }

    private void refreshCategoryCount() {
        categoryMapper.selectList(null).forEach(c -> {
            Long count = skillMapper.selectCount(new QueryWrapper<Skill>().eq("category_id", c.getId()));
            c.setSkillCount(count == null ? 0 : count.intValue());
            c.setUpdateTime(LocalDateTime.now());
            categoryMapper.updateById(c);
        });
    }

    private Map<String, Object> readJson(Path path) {
        if (path == null || !Files.isRegularFile(path)) return null;
        try { return JSON.parseObject(Files.readString(path)); }
        catch (Exception e) { log.debug("[seed] read json failed {}: {}", path, e.getMessage()); return null; }
    }
}
