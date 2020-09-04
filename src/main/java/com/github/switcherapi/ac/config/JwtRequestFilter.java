package com.github.switcherapi.ac.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.switcherapi.ac.service.JwtTokenService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
	
	private static final Logger logger = LogManager.getLogger(JwtRequestFilter.class);
	
	public static final String SWITCHER_AC = "SWITCHER_AC";
	
	public static final String AUTHORIZATION = "Authorization";

	public static final String BEARER = "Bearer ";
	
	@Autowired
	private JwtTokenService jwtTokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		final Optional<String> jwt = getJwtFromRequest(request);
		
		jwt.ifPresent(token -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Token {}", token);
			}
			
			try {
				if (validateToken(token, request)) {
					final UsernamePasswordAuthenticationToken authUser = 
							new UsernamePasswordAuthenticationToken(SWITCHER_AC, null, new ArrayList<>());
					authUser.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					
					SecurityContextHolder.getContext().setAuthentication(authUser);
				}
			} catch (IllegalArgumentException | MalformedJwtException | ExpiredJwtException | SignatureException e) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
		});
		
		filterChain.doFilter(request, response);
	}
	
	private boolean validateToken(String token, HttpServletRequest request) {
		if (request.getRequestURI().startsWith("/switcher"))
			return jwtTokenService.validateRelayToken(token);
		return jwtTokenService.validateAdminToken(token);
	}
	
	private static Optional<String> getJwtFromRequest(HttpServletRequest request) {
		final String bearerToken = request.getHeader(AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER)) {
			return Optional.of(bearerToken.substring(7));
		}
		return Optional.empty();
	}

}