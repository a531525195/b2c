package com.xiaolu.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.xiaolu.dao.interfaces.MailDaoInterface;
import com.xiaolu.pojo.Attachment;
import com.xiaolu.pojo.MailModel;
import com.xiaolu.pojo.User;
import com.xiaolu.service.interfaces.MailServiceInterface;

public class MailServiceImpl implements MailServiceInterface{
	private MailDaoInterface mailDao;
	@Override
	public void save(MailModel mail) {
		
	}

	@Override
	public void delete(MailModel mail) {
		
	}

	@Override
	public MailModel getMailById(int id) {
		return null;
	}

	@Override
	public List<MailModel> getInboxMail() {
		return null;
	}

	public MailDaoInterface getMailDao() {
		return mailDao;
	}

	public void setMailDao(MailDaoInterface mailDao) {
		this.mailDao = mailDao;
	}

	@Override
	public void send(MailModel mail) throws Exception{
		Properties props = System.getProperties();
		props.setProperty("mail.smtp.host", "smtp.qq.com");
		props.setProperty("mail.smtp.auth", "true");
		try {
			Session session = Session.getDefaultInstance(props);
			Message message = new MimeMessage(session);
			/**
			**发送者
			*/
			InternetAddress from = new InternetAddress();
			from.setAddress(mail.getSender());
			/**
			 * 接收者
			 */
			InternetAddress to = new InternetAddress();
			to.setAddress(mail.getReceiver());
			message.setFrom(from);
			message.setRecipient(RecipientType.TO, to);
			message.setSentDate(new Date());
			message.setSubject(mail.getTitle());
			
			Multipart multipart = new MimeMultipart(); 
			
			MimeBodyPart textBodyPart = new MimeBodyPart();
			textBodyPart.setText(mail.getContext());
			
			multipart.addBodyPart(textBodyPart);
			
			for(Iterator<Attachment> iterator = mail.getAttachment().iterator();iterator.hasNext();){
				try {
					MimeBodyPart fileBodyPart = new MimeBodyPart();
					fileBodyPart.attachFile(iterator.next().getFilepath());
					multipart.addBodyPart(fileBodyPart);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			message.setContent(multipart);
			Transport transport = session.getTransport("smtp");
			transport.connect("smtp.qq.com", mail.getSender(), "19900404");
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
			/**
			 * html消息体
			 */
			/*
			Multipart content = new MimeMultipart("mixed");//发送的消息
			message.setContent(content);
			
			BodyPart bodyPart = new MimeBodyPart();//发送消息的主体
			content.addBodyPart(bodyPart);
			
			Multipart bodyPartContent = new MimeMultipart("related");//发送消息主体的内容,其内容又是一个Multipart
			bodyPart.setContent(bodyPartContent);
			
			BodyPart htmlPart = new MimeBodyPart();//发送消息内容是一个html内容。
			bodyPartContent.addBodyPart(htmlPart);
			*/
			
		} catch (MessagingException e) {
			e.printStackTrace();
			try {
				throw e;
			} catch (MessagingException e1) {
				e1.printStackTrace();
			}
		}
		mailDao.save(mail);
	}

	@Override
	public Message[] receive(User user,String path) {
		Properties props = System.getProperties();
		Session session = Session.getDefaultInstance(props);
		session.setDebug(true);
		Message[] messages = null;
		Folder folder = null;
		Store store = null;
		try {
			store = session.getStore("pop3");
			store.connect("pop.qq.com","2283069875@qq.com" , "a19900404");
			folder = store.getFolder("INBOX");
			folder.open(Folder.READ_WRITE);
			messages = folder.getMessages();
			for(Message message:messages){
				if(message.getContent() instanceof Multipart){
					Multipart multipart = (Multipart) message.getContent();
					for(int i=0;i<multipart.getCount();i++){
						BodyPart bodypart = multipart.getBodyPart(i);
						String disposition = bodypart.getDisposition();
						if((null!=disposition)&&((disposition.equals(Part.ATTACHMENT))||(disposition.equals(Part.INLINE)))){
							String filename = bodypart.getFileName();
							System.out.println(filename);
							File file = new File(path,filename);
							FileOutputStream fos = new FileOutputStream(file);
							InputStream is = bodypart.getInputStream();
							int count = bodypart.getSize();
							byte[] buf = new byte[count];
							is.read(buf, 0, count);
							fos.write(buf, 0, count);
							is.close();
							fos.close();
						}else{
							if(bodypart.isMimeType("text/plain")){
								String content = bodypart.getContent().toString();
								System.out.println(content);
							}
						}
						
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				store.close();
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
		return messages;
	}

}
