package com.taskmind.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.auth.domain.OtpService;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"taskmind.auth.otp.max-attempts=2", "taskmind.ratelimit.auth-flow.capacity=100"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserJpaRepository users;
    @Autowired UserIdentityJpaRepository identities; @Autowired OtpChallengeJpaRepository challenges; @Autowired SessionJpaRepository sessions;
    @Autowired OtpService otpService; @Autowired JwtEncoder jwtEncoder; @Autowired JwtDecoder jwtDecoder;

    @Test
    void testProfileE2eBypassSeedsWorkingSuperAdminLogin() throws Exception {
        var result=mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"superadmin@taskmind.local\",\"password\":\"1\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("accessToken").asText()).isNotBlank();
        assertThat(result.getResponse().getCookie("taskmind_refresh")).isNotNull();
    }

    @Test
    void signupPersistsPendingUserHashedPasswordIdentityAndOtpThenVerificationActivatesAndIssuesTokens() throws Exception {
        var email=email();
        mvc.perform(post("/v1/auth/signup/email").contentType(MediaType.APPLICATION_JSON).content(signup(email))).andExpect(status().isAccepted());
        var user=users.findByPrimaryEmail(email).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(AuthJpaEnums.UserStatus.PENDING_VERIFICATION);
        assertThat(user.getPasswordHash()).startsWith("$2").doesNotContain("correct horse battery staple");
        assertThat(identities.findByTypeAndValue(AuthJpaEnums.IdentityType.EMAIL,email)).get().extracting(UserIdentityJpaEntity::isVerified).isEqualTo(false);
        assertThat(challenges.findByDestinationAndExpiresAtAfter(email, java.time.Instant.now())).hasSize(1);

        var tokens=verify(email,"1");
        assertThat(tokens.path("accessToken").asText()).isNotBlank();
        var accessJwt = jwtDecoder.decode(tokens.path("accessToken").asText());
        assertThat(accessJwt.getClaimAsStringList("roles")).containsExactly("MEMBER");
        assertThat(accessJwt.getClaimAsStringList("permissions")).containsExactlyInAnyOrder("project.read", "project.create");
        assertThat(accessJwt.getClaimAsStringList("authorities")).containsExactlyInAnyOrder("project.read", "project.create");
        assertThat(users.findByPrimaryEmail(email).orElseThrow().getStatus()).isEqualTo(AuthJpaEnums.UserStatus.ACTIVE);
        assertThat(identities.findByTypeAndValue(AuthJpaEnums.IdentityType.EMAIL,email).orElseThrow().isVerified()).isTrue();
        assertThat(sessions.findByUser_Id(user.getId())).singleElement().satisfies(s -> assertThat(s.getRefreshTokenHash()).isNotBlank());
    }

    @Test
    void invalidOtpDoesNotActivatePendingUser() throws Exception {
        var email=email(); mvc.perform(post("/v1/auth/signup/email").contentType(MediaType.APPLICATION_JSON).content(signup(email))).andExpect(status().isAccepted());
        mvc.perform(post("/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"otp\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(users.findByPrimaryEmail(email).orElseThrow().getStatus()).isEqualTo(AuthJpaEnums.UserStatus.PENDING_VERIFICATION);
    }


    @Test
    void validOtpStillWorksBeforeAttemptLimit() throws Exception {
        var email = email();
        mvc.perform(post("/v1/auth/signup/email").contentType(MediaType.APPLICATION_JSON).content(signup(email)))
                .andExpect(status().isAccepted());

        var tokens = verify(email, "1");

        assertThat(tokens.path("accessToken").asText()).isNotBlank();
        assertThat(users.findByPrimaryEmail(email).orElseThrow().getStatus()).isEqualTo(AuthJpaEnums.UserStatus.ACTIVE);
    }

    @Test
    void invalidOtpIncrementsAttemptsAndMaxFailuresInvalidateChallengeUntilNewOtpIsIssued() throws Exception {
        var email = email();
        mvc.perform(post("/v1/auth/signup/email").contentType(MediaType.APPLICATION_JSON).content(signup(email)))
                .andExpect(status().isAccepted());

        mvc.perform(post("/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"otp\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(challenges.findByDestinationAndExpiresAtAfter(email, java.time.Instant.now()))
                .singleElement()
                .satisfies(challenge -> assertThat(challenge.getAttemptCount()).isEqualTo(1));

        mvc.perform(post("/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"otp\":\"still-wrong\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(challenges.findByDestinationAndExpiresAtAfter(email, java.time.Instant.now()))
                .singleElement()
                .satisfies(challenge -> {
                    assertThat(challenge.getAttemptCount()).isEqualTo(2);
                    assertThat(challenge.getConsumedAt()).isNotNull();
                });

        mvc.perform(post("/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"otp\":\"1\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(users.findByPrimaryEmail(email).orElseThrow().getStatus()).isEqualTo(AuthJpaEnums.UserStatus.PENDING_VERIFICATION);

        otpService.dispatchOtp("EMAIL", email);
        assertThat(challenges.findByDestinationAndExpiresAtAfter(email, java.time.Instant.now()))
                .filteredOn(challenge -> challenge.getConsumedAt() == null)
                .singleElement()
                .satisfies(challenge -> assertThat(challenge.getAttemptCount()).isZero());

        var tokens = verify(email, "1");
        assertThat(tokens.path("accessToken").asText()).isNotBlank();
        assertThat(users.findByPrimaryEmail(email).orElseThrow().getStatus()).isEqualTo(AuthJpaEnums.UserStatus.ACTIVE);
    }

    @Test
    void loginAndMeUseSignedJwtAuthenticatedPrincipal() throws Exception {
        var email=email(); signupAndVerify(email); var tokens=login(email);
        mvc.perform(get("/v1/auth/me").header("Authorization","Bearer "+tokens.path("accessToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.displayName").value("Ada Lovelace"));
        mvc.perform(get("/v1/auth/me")).andExpect(status().isUnauthorized()).andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }


    @Test
    void meRejectsTokenWithWrongIssuer() throws Exception {
        String token = signedToken("wrong-issuer", List.of("taskmind-core-api"));

        mvc.perform(get("/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsTokenWithMissingAudience() throws Exception {
        String token = signedToken("taskmind-core-test", List.of());

        mvc.perform(get("/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsTokenWithWrongAudience() throws Exception {
        String token = signedToken("taskmind-core-test", List.of("other-api"));

        mvc.perform(get("/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesPersistentTokenAndInvalidatesPreviousToken() throws Exception {
        var email=email(); var old=signupAndVerifyResult(email); var oldRefresh=old.getResponse().getCookie("taskmind_refresh").getValue();
        var result=mvc.perform(post("/v1/auth/token/refresh").cookie(new Cookie("taskmind_refresh", oldRefresh)))
                .andExpect(status().isOk()).andReturn();
        var rotatedRefresh = result.getResponse().getCookie("taskmind_refresh").getValue();
        assertThat(rotatedRefresh).isNotEqualTo(oldRefresh);
        mvc.perform(post("/v1/auth/token/refresh").cookie(new Cookie("taskmind_refresh", oldRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesPersistentSession() throws Exception {
        var result=signupAndVerifyResult(email()); var refresh=result.getResponse().getCookie("taskmind_refresh").getValue();
        mvc.perform(post("/v1/auth/logout").cookie(new Cookie("taskmind_refresh", refresh))) .andExpect(status().isNoContent()).andExpect(cookie().maxAge("taskmind_refresh", 0));
        mvc.perform(post("/v1/auth/token/refresh").cookie(new Cookie("taskmind_refresh", refresh))) .andExpect(status().isUnauthorized());
    }

    private JsonNode signupAndVerify(String email) throws Exception { return json.readTree(signupAndVerifyResult(email).getResponse().getContentAsString()); }
    private org.springframework.test.web.servlet.MvcResult signupAndVerifyResult(String email) throws Exception { mvc.perform(post("/v1/auth/signup/email").contentType(MediaType.APPLICATION_JSON).content(signup(email))).andExpect(status().isAccepted()); return mvc.perform(post("/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"otp\":\"1\"}" )).andExpect(status().isOk()).andReturn(); }
    private JsonNode verify(String email,String otp) throws Exception { var r=mvc.perform(post("/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"otp\":\""+otp+"\"}" )).andExpect(status().isOk()).andReturn(); return json.readTree(r.getResponse().getContentAsString()); }
    private JsonNode login(String email) throws Exception { var r=mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"correct horse battery staple\"}" )).andExpect(status().isOk()).andReturn(); return json.readTree(r.getResponse().getContentAsString()); }
    private String signedToken(String issuer, List<String> audience) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .subject(UUID.randomUUID().toString())
                .claim("email", "jwt-test@example.com");
        if (!audience.isEmpty()) {
            claims.audience(audience);
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }
    private String signup(String email) { return "{\"email\":\""+email+"\",\"password\":\"correct horse battery staple\",\"displayName\":\"Ada Lovelace\"}"; }
    private String email() { return "user-"+UUID.randomUUID()+"@example.com"; }
}
