package com.tzl.llongagent.service;

import cn.hutool.core.util.RandomUtil;
import com.tzl.llongagent.entity.UserEntity;
import com.tzl.llongagent.exception.AuthException;
import com.tzl.llongagent.repository.UserRepository;
import com.tzl.llongagent.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public String register(String username, String rawPassword) {
        if (username == null || username.isBlank()) {
            throw new AuthException("用户名不能为空", HttpStatus.BAD_REQUEST);
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new AuthException("密码至少需要6位", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByUsername(username)) {
            throw new AuthException("账号已被使用", HttpStatus.CONFLICT);
        }

        UserEntity user = new UserEntity();
        user.setId(RandomUtil.randomString(6));
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        userRepository.save(user);

        return jwtTokenProvider.generateToken(user.getId());
    }

    public String login(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("请先注册", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthException("账号或密码错误", HttpStatus.UNAUTHORIZED);
        }

        return jwtTokenProvider.generateToken(user.getId());
    }
}
