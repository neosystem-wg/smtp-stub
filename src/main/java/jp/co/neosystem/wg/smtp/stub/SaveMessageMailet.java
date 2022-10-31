package jp.co.neosystem.wg.smtp.stub;

import com.ibm.icu.util.Output;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.QPDecoderStream;
import jp.co.neosystem.wg.smtp.stub.conf.SmtpStubConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimePart;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

public class SaveMessageMailet extends GenericMailet {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaveMessageMailet.class);

	@Override
	public void service(Mail mail) throws MessagingException {
		var sender = mail.getMaybeSender();
		LOGGER.info("SaveMessageMailet.service()");
		LOGGER.info("sender {}", sender.toString());

		var message = mail.getMessage();
		String directoryName = message.getMessageID().replaceAll("[<>]", "");
		SmtpStubConfig config = SmtpStubMain.getSmtpStubConfig();

		if (StringUtils.isNotEmpty(config.getDirectoryPrefixHeader())) {
			String[] headers = message.getHeader(config.getDirectoryPrefixHeader());
			if (headers != null && headers.length > 0) {
				DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
				directoryName = headers[0] + "_" + format.format(new Date());
			}
		}

		File directory = new File(config.getSaveDirectory(), directoryName);
		directory.mkdir();

		Counter counter = new Counter();

		try {
			File headersFile = new File(directory, "headers.txt");
			saveHeaders(message.getAllHeaders(), headersFile);

			Object content = message.getContent();
			if (content instanceof Multipart) {
				Multipart multipart = (Multipart) content;
				saveMultipart(counter, directory, multipart);
			} else {
				File contentFile = new File(directory, "content.dat");
				saveContent(content, contentFile);
			}
		} catch (IOException e) {
			LOGGER.warn(e.getMessage(), e);
		}
		return;
	}

	private void saveContent(Object content, File fileName) throws IOException, MessagingException {
		try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
			if (content instanceof String) {
				String str = (String) content;
				fileOutputStream.write(str.getBytes());
			} else {
				LOGGER.info("unknown type: {}", content.getClass().getName());
			}
		}
		return;
	}

	private void saveMultipart(Counter counter, File directory, Multipart multipart) throws MessagingException, IOException {
		for (int i = 0; i < multipart.getCount(); ++i) {
			BodyPart bodyPart = multipart.getBodyPart(i);
			saveMultipart(counter, directory, bodyPart);
		}
		return;
	}

	private void saveMultipart(Counter counter, File directory, BodyPart bodyPart) throws IOException, MessagingException {
		Object content = bodyPart.getContent();
		if (content instanceof Multipart) {
			String headerFileName = String.format("header%03d.txt", counter.getNewCount());
			File headerFile = new File(directory, headerFileName);
			saveHeaders(bodyPart.getAllHeaders(), headerFile);

			Multipart multipart = (Multipart) content;
			saveMultipart(counter, directory, multipart);
			return;
		}

		String headerFileName = null;
		String fileName = bodyPart.getFileName();
		if (StringUtils.isEmpty(fileName)) {
			int index = counter.getNewCount();
			fileName = String.format("content%03d.dat", index);
			headerFileName = headerFileName = String.format("header%03d.txt", index);
		} else {
			headerFileName = String.format("%s-header.txt", fileName);
		}

		File headerFile = new File(directory, headerFileName);
		saveHeaders(bodyPart.getAllHeaders(), headerFile);

		File contentFile = new File(directory, fileName);
		try (FileOutputStream fileOutputStream = new FileOutputStream(contentFile)) {
			saveMultipartImpl(bodyPart, fileOutputStream);
		}
		return;
	}

	private void saveMultipartImpl(BodyPart bodyPart, OutputStream stream) throws MessagingException, IOException {
		Object content = bodyPart.getContent();
		if (content instanceof String) {
			String tmp = (String) content;
			stream.write(tmp.getBytes());
		} else if (content instanceof BASE64DecoderStream) {
			BASE64DecoderStream base64DecoderStream = (BASE64DecoderStream) content;
			saveBase64DecoderStream(base64DecoderStream, stream);
		} else if (content instanceof QPDecoderStream) {
			QPDecoderStream qpDecoderStream = (QPDecoderStream) content;
			saveQPDecoderStream(qpDecoderStream, stream);
		} else {
			LOGGER.info("unknown type(saveMultipartImpl): {}", content.getClass().getName());
		}
		return;
	}

	private void saveBase64DecoderStream(BASE64DecoderStream base64DecoderStream, OutputStream stream) throws IOException {
		byte[] bytes = IOUtils.toByteArray(base64DecoderStream);
		stream.write(bytes);
		return;
	}

	private void saveQPDecoderStream(QPDecoderStream qpDecoderStream, OutputStream stream) throws IOException {
		byte[] bytes = IOUtils.toByteArray(qpDecoderStream);
		stream.write(bytes);
		return;
	}

	private void saveHeaders(Enumeration<Header> headers, File fileName) throws IOException {
		try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
			saveHeaders(headers, fileOutputStream);
		}
		return;
	}

	private void saveHeaders(Enumeration<Header> headers, OutputStream outputStream) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(outputStream);

		for (; headers.hasMoreElements(); ) {
			Header header = headers.nextElement();
			writer.write(header.getName() + ": " + header.getValue());
			writer.write("\n");
		}
		writer.flush();
		return;
	}

	private static class Counter {
		private int count;

		public Counter() {
			count = 0;
		}

		public int getNewCount() {
			++count;
			return count;
		}
	}
}
