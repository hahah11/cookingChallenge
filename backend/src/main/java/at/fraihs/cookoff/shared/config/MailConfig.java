package at.fraihs.cookoff.shared.config;

import at.fraihs.cookoff.shared.mail.MailMessageResolver;
import at.fraihs.cookoff.shared.mail.MailMessages;

import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * A standalone Thymeleaf engine for mail bodies, plus the executor that sends them.
 *
 * <p>Deliberately not {@code SpringTemplateEngine} via {@code spring-boot-starter-thymeleaf}:
 * mail templates need none of the Spring integration (no {@code WebContext}, no
 * {@code #authentication}), the starter would register an MVC view resolver this REST-only app
 * has no use for, and the only published Spring integration is {@code thymeleaf-spring6} — a
 * Spring Framework 6 artifact on a Framework 7 classpath.
 */
@Configuration
public class MailConfig {

    /**
     * Each body exists twice — {@code mail/x.html} and {@code mail/x.txt} — so the engine needs
     * one resolver per {@link TemplateMode}. Selection is by {@code resolvablePatterns} on the
     * full template name; the suffix is therefore empty, or Thymeleaf would look for
     * {@code mail/x.txt.txt}.
     *
     * <p>Wording comes from {@code mail/messages*.properties} via {@link MailMessageResolver}; the
     * template context's locale (the recipient's language) picks the bundle.
     */
    @Bean
    public TemplateEngine mailTemplateEngine(MailMessages mailMessages) {
        TemplateEngine engine = new TemplateEngine();
        engine.setMessageResolver(new MailMessageResolver(mailMessages));
        engine.addTemplateResolver(resolver(TemplateMode.HTML, "mail/*.html", 1));
        engine.addTemplateResolver(resolver(TemplateMode.TEXT, "mail/*.txt", 2));
        return engine;
    }

    /**
     * Bounded on purpose: mail is the one thing here that talks to a machine outside the compose
     * network, and {@code MailTransport} retries on top of that. A dedicated pool means a slow or
     * dead SMTP server can only ever exhaust these threads, never the servlet container's.
     */
    @Bean
    public TaskExecutor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        // A shutdown mid-send would otherwise drop a message that was about to go out.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    private static ITemplateResolver resolver(TemplateMode mode, String pattern, int order) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix("");
        resolver.setResolvablePatterns(Set.of(pattern));
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setOrder(order);
        return resolver;
    }
}
