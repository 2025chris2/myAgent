package com.tzl.llongagent.service;

import cn.hutool.core.util.RandomUtil;
import com.tzl.llongagent.entity.UserEntity;
import com.tzl.llongagent.repository.UserRepository;
import com.tzl.llongagent.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public String register(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
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
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        return jwtTokenProvider.generateToken(user.getId());
    }
}
