/**
 * 
 */
package org.iita.struts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.util.ServletContextAware;
import org.iita.security.model.User;
import org.iita.servlet.html.filter.KeepNotificationMessagesFilter;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

/**
 * @author mobreza
 */
@SuppressWarnings("serial")
public abstract class BaseAction extends ActionSupport implements Preparable, ServletContextAware {
	private static final String SESSION_NOTIFICATIONMESSAGES = "SESSION_NOTIFICATIONMESSAGES";
	private static final String SESSION_NEXTNOTIFICATIONMESSAGES = "SESSION_NEXTNOTIFICATIONMESSAGES";
	public static final String DOWNLOAD = "download";
	private Collection<String> notificationMessages, nextNotificationMessages;

	/**
	 * 
	 */
	private String path;
	private User user = null;
	private Boolean filterApplied = null;

	/**
	 * 
	 */
	public BaseAction() {
		super();
	}

	@Override
	public void setServletContext(ServletContext arg0) {
		this.path = arg0.getRealPath("/");
	}

	/*
	 * (non-Javadoc)
	 * @see com.opensymphony.xwork2.Preparable#prepare()
	 */
	@Override
	public void prepare() {

	}

	@Override
	public String execute() {
		return Action.SUCCESS;
	}
	
	public String tsexecute() {
		return Action.SUCCESS;
	}
	
	/**
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Get the user for which we are accessing data.
	 * 
	 * @see getPrincipal()
	 * @return the user
	 */
	public User getUser() {
		if (this.user == null) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null)
				return null;
			Object u = auth.getPrincipal();
			if (u instanceof User)
				this.user = (User) u;
		}
		return this.user;
	}

	/**
	 * Get currently logged in user.
	 * 
	 * @return the user
	 */
	public User getPrincipal() {
		User user = getUser();
		if (user == null)
			return null;
		if (user.getImpersonator() == null)
			return user;
		else
			return (User) user.getImpersonator().getPrincipal();
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	public String getVirtualDirectory() {
		ActionContext ac = ActionContext.getContext().getActionInvocation().getInvocationContext();
		HttpServletRequest request = (HttpServletRequest) ac.get(ServletActionContext.HTTP_REQUEST);
		String contextPath = request.getContextPath();
		return request == null ? null : (contextPath.equals("/") ? null : contextPath);
	}

	public String getHostname() {
		ActionContext ac = ActionContext.getContext().getActionInvocation().getInvocationContext();
		HttpServletRequest request = (HttpServletRequest) ac.get(ServletActionContext.HTTP_REQUEST);
		String host = request.getHeader("Host");
		return host;
	}

	/**
	 * General "cancel" button action that returns "goback" as action result.
	 * 
	 * @return
	 */
	public String goback() {
		LOG.info("Go back!");
		return "goback";
	}

	/**
	 * Get user's timezone
	 * 
	 * @return
	 */
	public TimeZone getTimezone() {
		TimeZone timezone = (TimeZone) ActionContext.getContext().get("WW_TRANS_I18N_TIMEZONE");
		if (timezone == null)
			timezone = TimeZone.getDefault();
		return timezone;
	}

	public synchronized Collection<String> getNotificationMessages() {
		return new ArrayList<String>(internalGetNotificationMessages());
	}

	@SuppressWarnings("unchecked")
	private Collection<String> internalGetNotificationMessages() {
		if (this.notificationMessages == null) {
			Map session = ActionContext.getContext().getSession();
			if (session != null) {
				// get from session
				this.notificationMessages = (Collection<String>) session.get(SESSION_NOTIFICATIONMESSAGES);
			}

			if (this.notificationMessages == null) {
				this.notificationMessages = new ArrayList<String>();
				if (isNotificationsFilterApplied())
					session.put(SESSION_NOTIFICATIONMESSAGES, this.notificationMessages);
				else
					session.remove(SESSION_NOTIFICATIONMESSAGES);
			}
		}

		return this.notificationMessages;
	}

	/**
	 * @return
	 */
	private boolean isNotificationsFilterApplied() {
		synchronized (this) {
			if (this.filterApplied == null) {
				HttpServletRequest request = (HttpServletRequest) ActionContext.getContext().get(ServletActionContext.HTTP_REQUEST);
				this.filterApplied = (Boolean) request.getAttribute(KeepNotificationMessagesFilter.FILTERAPPLIED);
				if (this.filterApplied == null)
					this.filterApplied = Boolean.FALSE;
			}
		}
		return this.filterApplied;
	}

	@SuppressWarnings("unchecked")
	private Collection<String> internalGetNextNotificationMessages() {
		if (this.nextNotificationMessages == null) {
			Map session = ActionContext.getContext().getSession();
			if (session != null) {
				// get from session
				this.nextNotificationMessages = (Collection<String>) session.get(SESSION_NEXTNOTIFICATIONMESSAGES);
			}

			if (this.nextNotificationMessages == null) {
				this.nextNotificationMessages = new ArrayList<String>();
				if (isNotificationsFilterApplied())
					session.put(SESSION_NEXTNOTIFICATIONMESSAGES, this.nextNotificationMessages);
				else
					session.remove(SESSION_NEXTNOTIFICATIONMESSAGES);
			}
		}

		return this.nextNotificationMessages;
	}

	public synchronized void addNotificationMessage(String aMessage) {
		internalGetNotificationMessages().add(aMessage);
		internalGetNextNotificationMessages().add(aMessage);
	}

	/**
	 * @see com.opensymphony.xwork2.ActionSupport#addActionError(java.lang.String)
	 */
	@Override
	public synchronized void addActionError(String anErrorMessage) {
		super.addActionError(anErrorMessage);
		internalGetNextNotificationMessages().add("<b>ERROR:</b> " + anErrorMessage);
	}

	/**
	 * @see com.opensymphony.xwork2.ActionSupport#clearErrorsAndMessages()
	 */
	@Override
	public synchronized void clearErrorsAndMessages() {
		super.clearErrorsAndMessages();
		internalGetNotificationMessages().clear();
		internalGetNextNotificationMessages().clear();
	}

	/**
	 * @see com.opensymphony.xwork2.ActionSupport#addActionMessage(java.lang.String)
	 */
	@Override
	public synchronized void addActionMessage(String message) {
		super.addActionMessage(message);
		internalGetNextNotificationMessages().add("<b>NOTICE:</b> " + message);
	}
}