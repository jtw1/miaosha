package org.example.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.security.MD5Encoder;
import org.example.controller.viewObject.UserVO;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.response.CommonReturnType;
import org.example.service.UserService;
import org.example.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Base64.Decoder;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @date 2021/4/12-22:08
 */
@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")     //解决跨域问题
public class UserController extends BaseController{
    @Autowired
    private UserService userService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RedisTemplate redisTemplate;

    //用户登陆接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telephone")String telephone,
                                  @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验 手机和密码不能为空
        if (org.apache.commons.lang3.StringUtils.isEmpty(telephone) ||
                StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登录服务 检验用户登录是否合法
        UserModel userModel=userService.validateLogin(telephone,this.EncodeByMd5(password));

        //将登录凭证加入到用户登陆成功的session内
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USR",userModel);

        /**
         * // 修改成若用户登录验证成功后，将对应的登录信息和登录凭证一起存入redis
         *
        // 生成登陆凭证token，UUID
        String uuidToken = UUID.randomUUID().toString();
         uuidToken=uuidToken.replace("-","");
        //建立token和用户登录态之间的联系
        redisTemplate.opsForValue().set(uuidToken,userModel);
        //设置过期时间
        redisTemplate.expire(uuidToken,1, TimeUnit.HOURS);
        //最后下发token
        return CommonReturnType.create(uuidToken);
         */
        //返回给前端一个正确的信息
        return CommonReturnType.create(null);

    }

    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telephone")String telephone,
                                     @RequestParam(name = "otpCode")String otpCode,
                                     @RequestParam(name = "name")String name,
                                     @RequestParam(name = "gender")Integer gender,
                                     @RequestParam(name = "age")Integer age,
                                     @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //验证手机号与对应的otpCode相符合
        //String otpCodeInSession=(String)this.httpServletRequest.getSession().getAttribute(telephone);
        String otpCodeInSession=(String)this.httpServletRequest.getParameter("otpCode");
        if (!com.alibaba.druid.util.StringUtils.equals(otpCode,otpCodeInSession)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短信验证码不符合");
        }

        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender);
        userModel.setAge(age);
        userModel.setTelephone(telephone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.EncodeByMd5(password));

        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    public String EncodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定计算方法
        MessageDigest md5=MessageDigest.getInstance("MD5");
        // BASE64Encoder base64en = new BASE64Encoder();   jdk8支持，jdk12不支持
        Encoder encoder=Base64.getEncoder();
        //加密字符串
        String newStr=encoder.encodeToString(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }

    //用户获取otp短信接口
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name="telephone")String telephone){
        //需要按照一定的规则生成otp验证码
        Random random = new Random();
        int randomInt=random.nextInt(99999);
        randomInt += 10000;
        String otpCode=String.valueOf(randomInt);

        //将otp验证码同对应用户的手机号关联,使用httpsession的方式绑定他的手机号和otpCode，也可以使用redis
        httpServletRequest.getSession().setAttribute(telephone,otpCode);

        //将otp验证码通过短信通道发送给用户，省略
        System.out.println("telephone="+telephone+"&otpCode="+otpCode);
        return CommonReturnType.create(null);
    }


    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name="id") Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        //当获取的用户信息不存在时
        if (userModel==null){
            //userModel.setEncrptPassword("wqwqw");
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //将核心领域用户模型对象转化为可供UI使用的viewObject
        UserVO userVO=convertFromModel(userModel);
        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    /**
     * 将核心领域用户模型对象转化为可供UI使用的viewObject
     * @param userModel
     * @return
     */
    private UserVO convertFromModel(UserModel userModel){
        if (userModel==null){
            return null;
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }
}
