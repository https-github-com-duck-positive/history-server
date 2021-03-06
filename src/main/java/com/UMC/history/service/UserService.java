package com.UMC.history.service;

import com.UMC.history.DTO.QuizDTO;
import com.UMC.history.DTO.TokenDTO;
import com.UMC.history.DTO.UserDTO;
import com.UMC.history.entity.strongEntity.PostEntity;
import com.UMC.history.entity.strongEntity.UserEntity;
import com.UMC.history.entity.weekEntity.CommentEntity;
import com.UMC.history.entity.weekEntity.LikeEntity;
import com.UMC.history.entity.weekEntity.QuizEntity;
import com.UMC.history.entity.weekEntity.RefreshToken;
import com.UMC.history.jwt.TokenProvider;
import com.UMC.history.repository.*;
import com.UMC.history.util.Authority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.List;

@Service
public class UserService {

    private UserRepository userRepository;
    private QuizRepository quizRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManagerBuilder authenticationManagerBuilder;
    private TokenProvider tokenProvider;
    private RefreshTokenRepository refreshTokenRepository;
    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private LikeRepository likeRepository;

    public UserService(UserRepository userRepository,PasswordEncoder passwordEncoder,
                       AuthenticationManagerBuilder authenticationManagerBuilder,TokenProvider tokenProvider,
                       RefreshTokenRepository refreshTokenRepository,QuizRepository quizRepository,
                       PostRepository postRepository, CommentRepository commentRepository, LikeRepository likeRepository){
        this.userRepository = userRepository;
        this.quizRepository=quizRepository;
        this.passwordEncoder=passwordEncoder;
        this.authenticationManagerBuilder =authenticationManagerBuilder;
        this.tokenProvider =tokenProvider;
        this.refreshTokenRepository =refreshTokenRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }


    public void saveUserData(@RequestBody UserDTO.User user){
        //????????? ??????
        Authority Auth=Authority.ROLE_USER;
        if (user.getAuthority().equals("admin")){
            Auth=Authority.ROLE_ADMIN;
        }
        //encode ??? ??????????????? ????????? ??? ??? ??????
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword); // password??? ?????? setting?????? ?????? UserEntity??? @Setter??????
        UserEntity userEntity = UserEntity.builder()
                .userId(user.getId())
                .nickName(user.getNickName())
                .password(user.getPassword())
                .authority(Auth)
                .build();
        userRepository.save(userEntity);
    }

    public boolean nickNameExist(String nickName) {
        return userRepository.existsByNickName(nickName);
    }

    public Boolean changeNickName(UserDTO.User user) {
        UserEntity hasUserId = userRepository.findByUserId(user.getId());// jwt
        if(hasUserId!=null){
            hasUserId.changeNickName(user.getNickName());
            userRepository.save(hasUserId);
            return true;
        }else return false;
    }

    public TokenDTO login(UserDTO.User user) {
        UserEntity findUser = userRepository.findByUserId(user.getId());
        if(findUser == null){
            TokenDTO token =new TokenDTO();
            token.setGrantType("Id Error");
            return token;
        }

        if(!passwordEncoder.matches(user.getPassword(), findUser.getPassword())){ // ?????? ????????? password??? ????????? ????????? ??????????????? ?????????.
            TokenDTO token =new TokenDTO();
            token.setGrantType("Password Error");
            return token;
        }
        // 1. Login ID/PW ??? ???????????? AuthenticationToken ??????
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getId(), user.getPassword());
        // 2. ????????? ?????? (????????? ???????????? ??????) ??? ??????????????? ??????
        //    authenticate ???????????? ????????? ??? ??? CustomUserDetailsService ?????? ???????????? loadUserByUsername ???????????? ?????????
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // 3. ?????? ????????? ???????????? JWT ?????? ??????
        TokenDTO tokenDto = tokenProvider.generateTokenDto(authentication);
        // 4. RefreshToken ??????
        RefreshToken refreshToken = RefreshToken.builder()
                .key(authentication.getName())
                .value(tokenDto.getRefreshToken())
                .build();
        refreshTokenRepository.save(refreshToken);
        // 5. ?????? ??????
        return tokenDto;
    }

    public boolean checkUserId(String userId){
        return userRepository.existsByUserId(userId);
    }


    public TokenDTO reissue(TokenDTO tokenRequestDto) { //?????????
        // 1. Refresh Token ??????
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("Refresh Token ??? ???????????? ????????????.");
        }

        // 2. Access Token ?????? Member ID ????????????
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. ??????????????? Member ID ??? ???????????? Refresh Token ??? ?????????
        RefreshToken refreshToken = refreshTokenRepository.findByKeyId(authentication.getName())
                .orElseThrow(() -> new RuntimeException("???????????? ??? ??????????????????."));

        // 4. Refresh Token ??????????????? ??????
        if (!refreshToken.getValue().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("????????? ?????? ????????? ???????????? ????????????.");
        }

        // 5. ????????? ?????? ??????
        TokenDTO tokenDto = tokenProvider.generateTokenDto(authentication);

        // 6. ????????? ?????? ????????????
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(newRefreshToken);

        // ?????? ??????
        return tokenDto;
    }

    public void registerQuiz(QuizDTO.Quiz quiz){
        QuizEntity quizEntity = QuizEntity.builder()
                .category(quiz.getCategory())
                .question(quiz.getQuestion())
                .answer(quiz.getAnswer())
                .solution(quiz.getSolution())
                .build();
        quizRepository.save(quizEntity);
    }

    public UserEntity informationAboutUser(Principal principal) {
        return userRepository.findByUserId(principal.getName());
    }

    public Boolean deleteUser(String password, Principal principal) {
        UserEntity user = userRepository.findByUserId(principal.getName());
        if(passwordEncoder.matches(password, user.getPassword())){
            List<CommentEntity> comment = commentRepository.findByUser(user);
            if(comment!=null){
                comment.stream().forEach(o ->
                        commentRepository.deleteById(o.getCommentIdx()));
            }
            List<LikeEntity> likes = likeRepository.findByUserOrderByCreatedDateDesc(user);
            if(likes!=null){
                likes.stream().forEach(o ->
                        likeRepository.deleteById(o.getLikeIdx()));
            }
            List<PostEntity> posts = postRepository.findByUserOrderByCreatedDateDesc(user);
            if(posts!=null){
                posts.stream().forEach(o ->
                        postRepository.deleteById(o.getPostIdx()));
            }
            RefreshToken token = refreshTokenRepository.findByKeyId(user.getUserId()).get();
            refreshTokenRepository.delete(token);
            userRepository.deleteById(user.getUserIdx());
            return true;
        }else return false;

    }

    public Boolean deleteQuiz(Long quizIdx){
        if (quizRepository.existsByQuizIdx(quizIdx)){
            quizRepository.deleteById(quizIdx);
            return true;
        }
        else return false;
    }
}
