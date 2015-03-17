package ru.orangesoftware.financisto2.export.sync;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Lyubomyr on 17.03.2015.
 */
public class AccessToken extends BaseResponse {

    @SerializedName("access_token")
    @Expose
    private String accessToken;

    @SerializedName("token_type")
    @Expose
    private String tokenType;

    @SerializedName("expires_in")
    @Expose
    private Integer expiresIn;

    @SerializedName("refresh_token")
    @Expose
    private String refreshToken;

    @Expose
    private String userName;

    @Expose
    private String userGroupId;

    @SerializedName("as:client_id")
    @Expose
    private String asClientId;

    @SerializedName(".issued")
    @Expose
    private String Issued;

    @SerializedName(".expires")
    @Expose
    private String Expires;

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        // OAuth requires uppercase Authorization HTTP header value for token type
        if (!Character.isUpperCase(tokenType.charAt(0))) {
            tokenType = Character.toString(tokenType.charAt(0)).toUpperCase() + tokenType.substring(1);
        }

        return tokenType;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserGroupId() {
        return userGroupId;
    }

    public String getAsClientId() {
        return asClientId;
    }

    public String getIssued() {
        return Issued;
    }

    public String getExpires() {
        return Expires;
    }

    @Override
    public String toString() {

        if (super.getError() != null) {
            return "AccessToken{error='" + super.getError() + "'}";
        }

        return "AccessToken{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", refreshToken='" + refreshToken + '\'' +
                ", username='" + userName + '\'' +
                ", userGroupId='" + userGroupId + '\'' +
                '}';
    }
}