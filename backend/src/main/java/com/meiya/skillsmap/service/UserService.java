package com.meiya.skillsmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meiya.skillsmap.entity.User;

public interface UserService extends IService<User> {

    /**
     * 通过 username 查询
     */
    User getByUsername(String username);

    /**
     * 校验密码（BCrypt）
     */
    boolean checkPassword(User user, String raw);
}
