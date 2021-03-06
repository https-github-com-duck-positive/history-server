package com.UMC.history.entity.strongEntity;


import com.UMC.history.util.Authority;
import com.UMC.history.util.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user")
@DynamicInsert
public class UserEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userIdx;

    @Column(nullable = false, length = 100)
    private String nickName;

    @Column(nullable = false, length = 100)
    private String userId;

    @JsonIgnore // 불러오지 않기
    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String profileImgUrl;

    @JsonIgnore
    @Column(columnDefinition = "boolean default false")
    private boolean lockScreen;

    @JsonIgnore
    @Column(columnDefinition = "boolean default false")
    private boolean autoLoginFlag;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Authority authority;

    @Builder
    public UserEntity(String nickName, String userId, String password, String profileImgUrl, boolean lockScreen, boolean autoLoginFlag,Authority authority) {
        this.nickName = nickName;
        this.userId = userId;
        this.password = password;
        this.profileImgUrl = profileImgUrl;
        this.lockScreen = lockScreen;
        this.autoLoginFlag = autoLoginFlag;
        this.authority = authority;

    }

    public void changeNickName(String nickName) {
        this.nickName = nickName;
    }
}
