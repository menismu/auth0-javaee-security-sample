package com.auth0.example;

import com.auth0.AuthenticationController;
import com.auth0.IdentityVerificationException;
import com.auth0.Tokens;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ApplicationScoped
@AutoApplySession
public class Auth0LoginAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private Auth0AuthenticationConfig config;

    @Inject
    private AuthenticationController authenticationController;

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest req, HttpServletResponse res,
        HttpMessageContext context) {
        if (isCallbackRequest(req)) {
            CredentialValidationResult result = CredentialValidationResult.NOT_VALIDATED_RESULT;

            try {
                Tokens tokens = authenticationController.handle(req);
                Auth0JwtCredential credential = new Auth0JwtCredential(tokens.getIdToken());
                result = identityStoreHandler.validate(credential);
            } catch (IdentityVerificationException e) {
                e.printStackTrace(); //TODO: Add proper logging
            }

            return context.notifyContainerAboutLogin(result);
        }

        if (context.getCallerPrincipal() == null) {
            return context.redirect(createLoginUrl(req));
        } else {
            return context.doNothing();
        }
    }

    private boolean isCallbackRequest(HttpServletRequest request) {
        return (request.getRequestURI().equals(config.getCallbackUri()) && request.getParameter("code") != null);
    }

    private String createLoginUrl(final HttpServletRequest req) {
        String redirectUri =
            req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + config.getCallbackUri();
        String authorizeUrl = authenticationController.buildAuthorizeUrl(req, redirectUri)
            .withAudience(String.format("https://%s/userinfo", config.getDomain()))
            .withScope(config.getScope())
            .build();
        return authorizeUrl;
    }

}