package jp.co.neosystem.wg.smtp.stub;

import jp.co.neosystem.wg.smtp.stub.conf.ResponseConfig;
import jp.co.neosystem.wg.smtp.stub.conf.SmtpStubConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.core.MailAddress;
import org.apache.james.protocols.smtp.MailEnvelope;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.hook.HookResult;
import org.apache.james.protocols.smtp.hook.HookReturnCode;
import org.apache.james.protocols.smtp.hook.MessageHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MyMessageHook implements MessageHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyMessageHook.class);

    @Override
    public HookResult onMessage(SMTPSession smtpSession, MailEnvelope mailEnvelope) {

        LOGGER.info("MyMessageHook.onMessage()");

        List<MailAddress> recipients = mailEnvelope.getRecipients();
        for (MailAddress recipient: recipients) {
            ResponseConfig config = getResposneConfig(SmtpStubMain.getSmtpStubConfig(), recipient);
            if (config != null) {
                HookResult result = new HookResult.Builder()
                        .hookReturnCode(HookReturnCode.deny())
                        .smtpReturnCode(String.valueOf(config.getReturnCode()))
                        .smtpDescription(config.getSmtpDescription())
                        .build();
                return result;
            }
        }
        return HookResult.DECLINED;
    }

    public ResponseConfig getResposneConfig(SmtpStubConfig configs, MailAddress mailAddress) {
        if (configs == null || configs.getResponse() == null) {
            return null;
        }
        if (mailAddress == null) {
            return null;
        }
        for (ResponseConfig config: configs.getResponse()) {
            String dst = config.getDst();
            if (StringUtils.isEmpty(dst)) {
                continue;
            }
            if (dst.equals(mailAddress.asString())) {
                return config;
            }
        }
        return null;
    }
}
