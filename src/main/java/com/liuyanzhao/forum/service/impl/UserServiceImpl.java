package com.liuyanzhao.forum.service.impl;

import com.liuyanzhao.forum.entity.Authority;
import com.liuyanzhao.forum.entity.Bind;
import com.liuyanzhao.forum.entity.BindType;
import com.liuyanzhao.forum.entity.User;
import com.liuyanzhao.forum.enums.MessageStatusEnum;
import com.liuyanzhao.forum.exception.CustomException;
import com.liuyanzhao.forum.repository.BindRepository;
import com.liuyanzhao.forum.repository.MessageRepository;
import com.liuyanzhao.forum.repository.UserRepository;
import com.liuyanzhao.forum.service.NoticeService;
import com.liuyanzhao.forum.service.RelationshipService;
import com.liuyanzhao.forum.service.UserService;
import com.liuyanzhao.forum.util.BeanUtils;
import com.liuyanzhao.forum.vo.UserSessionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;


/**
 * @author 言曌
 * @date 2018/3/20 下午5:32
 */

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private BindRepository bindRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NoticeService noticeService;


    @Transactional
    @Override
    public User saveUser(User user) {

        if (user.getId() != null) {
            //更新用户
            User originalUser = getUserById(user.getId());
            //这个地方是个坑，因为 getAuthorities 得到的是角色名称，不是角色对象
            BeanUtils.copyProperties(user, originalUser);
            User result = null;
            return userRepository.save(originalUser);
        } else {
            //创建用户
            return userRepository.save(user);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeUser(Integer id) {
        userRepository.delete(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeUsersInBatch(List<User> users) {
        userRepository.delete(users);
    }


    @Override
    public User getUserById(Integer id) {
        return userRepository.findOne(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> listUsersByNicknameLike(String nickname, Pageable pageable) {
        // 模糊查询
        nickname = "%" + nickname + "%";
        Page<User> users = userRepository.findByNicknameLike(nickname, pageable);
        return users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        //判断用户状态是否可以登录
        if (user != null) {
            //正常
            if ("normal".equals(user.getStatus())) {
                return user;
            } else {
                throw new CustomException("该用户已禁止登录");
            }
        }
        return null;
    }

    @Override
    public List<User> listUsersByUsername(Collection<String> username) {
        return userRepository.findByUsernameIn(username);
    }

    @Override
    public UserSessionVO getUserSessionVO(User user) {
        UserSessionVO userVO = new UserSessionVO();
        BeanUtils.copyProperties(user, userVO);
        try {
            userVO.setMessageSize(messageRepository.countByUserAndStatus(user, MessageStatusEnum.NOT_READ_MESSAGE.getCode()));
            userVO.setNoticeSize(noticeService.countNotReadNotices(user));
        } catch (Exception e) {
            logger.error("redis服务故障", e);
        }
        return userVO;
    }


    @Override
    public User getUserByCondition(BindType bindType, String identifier) {
        Bind bind = bindRepository.findByBindTypeAndIdentifier(bindType, identifier);
        if (bind == null) {
            return null;
        } else {
            return bind.getUser();
        }
    }

    //    @Cacheable(value = "12h", key = "'user:list'")
    @Override
    public List<User> getUserArticleRank() {
        //获得排行榜前10名的用户，每12小时刷新一次
//        List<User> userList = userRepository.findTop10UserBySevenDayArticleSize();
        List<User> userList = userRepository.findAll();
//        List<BigInteger> countList = userRepository.findTop10CountBySevenDayArticleSize();
//        for (int i = 0; i < userList.size(); i++) {
//            Integer count = Integer.valueOf(countList.get(i).toString());
//            userList.get(i).setArticleSize(count);
//        }
//        System.out.println("进来了");
        return userList;
    }

    @Override
    public Page<User> listUsersByRoleOrKeywords(List<Authority> authorityList, String keywords, Pageable pageable) {
        return userRepository.findByAuthoritiesContainsAndUsernameLikeOrAuthoritiesContainsAndNicknameLike(authorityList, "%" + keywords + "%", authorityList, "%" + keywords + "%", pageable);
    }

    @Override
    public Integer countUserByAuthority(List<Authority> authorityList) {
        return userRepository.countByAuthoritiesContains(authorityList);
    }

    @Override
    public Long countUser() {
        return userRepository.count();
    }

    @Transactional
    @Override
    public void increaseReputation(User user, Integer value) {
        Integer reputation = user.getReputation();
        user.setReputation(reputation + value);
        userRepository.save(user);
    }


}
