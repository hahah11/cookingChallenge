package at.fraihs.cookoff.shared.mail;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.AbstractMessageResolver;

/**
 * Feeds {@code #{key(args)}} expressions from {@link MailMessages}. The engine has no Spring
 * {@code MessageSource} to lean on (see {@code MailConfig}), and Thymeleaf's own resolver looks
 * for a {@code .properties} file per template, which would mean one bundle per mail and per
 * fragment instead of one shared file.
 */
public class MailMessageResolver extends AbstractMessageResolver {

    private final MailMessages messages;

    public MailMessageResolver(MailMessages messages) {
        this.messages = messages;
    }

    @Override
    public String resolveMessage(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        return messages.find(key, context.getLocale(), messageParameters == null ? new Object[0] : messageParameters);
    }

    @Override
    public String createAbsentMessageRepresentation(ITemplateContext context, Class<?> origin, String key,
                                                    Object[] messageParameters) {
        return "??" + key + "_" + context.getLocale() + "??";
    }
}
