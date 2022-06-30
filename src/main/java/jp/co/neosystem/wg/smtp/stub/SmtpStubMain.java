package jp.co.neosystem.wg.smtp.stub;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import jp.co.neosystem.wg.smtp.stub.conf.SmtpStubConfig;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.james.*;
import org.apache.james.data.UsersRepositoryModuleChooser;
import org.apache.james.modules.data.MemoryUsersRepositoryModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.apache.james.modules.server.MailetContainerModule;
import org.apache.james.modules.server.RawPostDequeueDecoratorModule;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class SmtpStubMain implements JamesServerMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpStubMain.class);

    public static final Module SMTP_ONLY_MODULE = Modules.combine(
            MemoryJamesServerMain.IN_MEMORY_SERVER_MODULE,
            new ProtocolHandlerModule(),
            new SMTPServerModule(),
            new RawPostDequeueDecoratorModule(),
            binder -> binder.bind(MailetContainerModule.DefaultProcessorsConfigurationSupplier.class)
                    .toInstance(BaseHierarchicalConfiguration::new));

    private static SmtpStubConfig smtpStubConfig;

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("c", true, "config file");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.warn(e.getMessage(), e);
            return;
        }

        String configFileStr = "smtp-stub.yaml";
        if (cmd.hasOption("c")) {
            configFileStr = cmd.getOptionValue("c");
        }
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer);
        File configFile = new File(configFileStr);
        if (!configFile.exists()) {
            LOGGER.info("config file does not exist (file: {})", configFile.toString());
            return;
        }
        try (var fileInputStream = new FileInputStream(configFile);
             BufferedInputStream stream = new BufferedInputStream(fileInputStream)) {
            smtpStubConfig = yaml.loadAs(stream, SmtpStubConfig.class);
        }
        dumpResponseConfig(smtpStubConfig);

        ExtraProperties.initialize();

        MemoryJamesConfiguration configuration = MemoryJamesConfiguration.builder()
                .useWorkingDirectoryEnvProperty()
                .build();

        GuiceJamesServer server = createServer(configuration)
                .combineWith(new FakeSearchMailboxModule());

        JamesServerMain.main(server);
        return;
    }

    private static void dumpResponseConfig(SmtpStubConfig configs) {
        if (configs == null || configs.getResponse() == null) {
            return;
        }
        for (var config: configs.getResponse()) {
            LOGGER.info(config.toString());
        }
        return;
    }

    public static GuiceJamesServer createServer(MemoryJamesConfiguration configuration) {
        return GuiceJamesServer.forConfiguration(configuration)
                .combineWith(SMTP_ONLY_MODULE)
                .combineWith(new UsersRepositoryModuleChooser(new MemoryUsersRepositoryModule())
                        .chooseModules(configuration.getUsersRepositoryImplementation()));
    }

    public static SmtpStubConfig getSmtpStubConfig() {
        return smtpStubConfig;
    }
}
