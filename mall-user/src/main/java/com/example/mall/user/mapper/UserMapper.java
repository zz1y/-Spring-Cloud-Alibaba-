package com.example.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
