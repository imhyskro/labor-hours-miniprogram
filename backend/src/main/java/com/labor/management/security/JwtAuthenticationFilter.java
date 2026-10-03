package com.labor.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labor.management.common.CommonResult;
import com.labor.management.common.ResultCode;
import com.labor.management.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 *
 * <p>从请求头 Authorization 中提取 Bearer Token，
 * 解析 username 后从数据库加载用户信息存入 SecurityContextHolder。
 * Token 无效或过期时直接写回响应，不抛出异常。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    @Value("${jwt.header}")
    private String headerName;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(headerName);

        // 规范化前缀，确保为 "Bearer "（恰好一个空格），兼容配置中是否含尾随空格
        String prefix = tokenPrefix.trim() + " ";

        // 无 Token 或格式不符，直接放行，由 Security 后续判定是否需要认证
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(prefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取 Token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(prefix.length());

        try {
            if (!jwtUtil.validateToken(token)) {
                writeUnauthorized(response, ResultCode.UNAUTHORIZED, "Token 无效或已过期");
                return;
            }

            String username = jwtUtil.parseUsername(token);
            if (!StringUtils.hasText(username)) {
                writeUnauthorized(response, ResultCode.UNAUTHORIZED, "Token 解析失败");
                return;
            }

            // 已认证则跳过
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            log.warn("JWT 异常: {}", e.getMessage());
            writeUnauthorized(response, ResultCode.UNAUTHORIZED, "Token 无效或已过期");
        } catch (Exception e) {
            log.error("JWT 过滤器处理异常", e);
            writeUnauthorized(response, ResultCode.UNAUTHORIZED, "认证失败");
        }
    }

    /**
     * 写回未认证响应（不抛异常）
     */
    private void writeUnauthorized(HttpServletResponse response,
                                  ResultCode resultCode,
                                  String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        CommonResult<Void> result = CommonResult.failed(resultCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
