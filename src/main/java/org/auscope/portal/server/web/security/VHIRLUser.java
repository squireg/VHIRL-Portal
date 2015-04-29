package org.auscope.portal.server.web.security;

import org.auscope.portal.core.server.security.oauth2.PortalUser;
import org.springframework.security.core.GrantedAuthority;

import java.net.URI;
import java.util.Collection;

/**
 * Created by wis056 on 23/04/2015.
 */
public class VHIRLUser extends PortalUser {
    private URI link;

    public URI getLink() {
        return link;
    }

    public void setLink(URI link) {
        this.link = link;
    }

    public VHIRLUser(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, true, true, true, true, authorities);
    }

    public VHIRLUser(String username, String password, boolean enabled, boolean accountNonExpired,
                      boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
    }


}
