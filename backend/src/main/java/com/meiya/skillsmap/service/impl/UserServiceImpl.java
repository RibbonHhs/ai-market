package com.meiya.skillsmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public User getByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public boolean checkPassword(User user, String raw) {
        if (user == null || raw == null) {
            return false;
        }
        return ENCODER.matches(raw, user.getPassword());
    }

    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }
}
