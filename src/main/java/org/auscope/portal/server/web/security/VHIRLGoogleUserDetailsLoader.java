package org.auscope.portal.server.web.security;

import org.auscope.portal.core.server.security.oauth2.GoogleOAuth2UserDetailsLoader;
import org.auscope.portal.core.server.security.oauth2.PortalUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by wis056 on 23/04/2015.
 */
public class VHIRLGoogleUserDetailsLoader extends GoogleOAuth2UserDetailsLoader {

    /**
     * Creates a new GoogleOAuth2UserDetailsLoader that will assign defaultRole to every user
     * as a granted authority.
     * @param defaultRole
     */
    public VHIRLGoogleUserDetailsLoader(String defaultRole) {
        super(defaultRole);
    }

    /**
     * Creates a new GoogleOAuth2UserDetailsLoader that will assign defaultRole to every user
     * AND any authorities found in rolesByUser if the ID matches the current user ID
     * @param defaultRole
     * @param rolesByUser
     */
    public VHIRLGoogleUserDetailsLoader(String defaultRole, Map<String, List<String>> rolesByUser) {
        super(defaultRole, rolesByUser);
    }

    protected void applyInfoToUser(VHIRLUser user,  Map<String, Object> userInfo) {
        user.setEmail(userInfo.get("email").toString());
        user.setFullName(userInfo.get("name").toString());
        try {
            URI userProfileLink = new URI(userInfo.get("link").toString());
            user.setLink(userProfileLink);
        } catch (URISyntaxException e) {
            // Just don't load anything.
        }
    }

    @Override
    public UserDetails createUser(String id, Map<String, Object> userInfo) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(defaultRole));
        if (rolesByUser != null) {
            List<SimpleGrantedAuthority> additionalAuthorities = rolesByUser.get(id);
            if (additionalAuthorities != null) {
                authorities.addAll(additionalAuthorities);
            }
        }

        VHIRLUser newUser = new VHIRLUser(id, "", authorities);
        applyInfoToUser(newUser, userInfo);
        return newUser;
    }

    @Override
    public UserDetails updateUser(UserDetails userDetails,
                                  Map<String, Object> userInfo) {

        if (userDetails instanceof VHIRLUser) {
            applyInfoToUser((VHIRLUser) userDetails, userInfo);
        }

        return userDetails;
    }

}
