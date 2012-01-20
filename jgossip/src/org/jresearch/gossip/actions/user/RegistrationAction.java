/*
 * $$Id: RegistrationAction.java,v 1.3 2005/06/07 12:32:29 bel70 Exp $$
 *
 * ***** BEGIN LICENSE BLOCK *****
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License
 * at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and
 * limitations under the License.
 *
 * The Original Code is JGossip forum code.
 *
 * The Initial Developer of the Original Code is the JResearch, Org.
 * Portions created by the Initial Developer are Copyright (C) 2004
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *              Dmitry Belov <bel@jresearch.org>
 *
 * ***** END LICENSE BLOCK ***** */
/*
 * Created on 01.06.2003
 *
 */
package org.jresearch.gossip.actions.user;

import java.sql.SQLException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.jresearch.gossip.IConst;
import org.jresearch.gossip.actions.BaseAction;
import org.jresearch.gossip.beans.user.User;
import org.jresearch.gossip.configuration.Configurator;
import org.jresearch.gossip.dao.UserDAO;
import org.jresearch.gossip.exception.SystemException;
import org.jresearch.gossip.forms.ProfileForm;

/**
 * DOCUMENT ME!
 * 
 * @author Bel
 */
public class RegistrationAction extends BaseAction {

	/**
	 * DOCUMENT ME!
	 * 
	 * @param mapping
	 *            DOCUMENT ME!
	 * @param form
	 *            DOCUMENT ME!
	 * @param request
	 *            DOCUMENT ME!
	 * @param response
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
	public ActionForward process(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws SystemException {
		if (IConst.VALUES.FALSE.equals(Configurator.getInstance().get(
				IConst.CONFIG.ENABLE_FORUM_SIGN_ON))
				|| IConst.VALUES.FALSE.equals(Configurator.getInstance().get(
						IConst.CONFIG.ENABLE_FORUM_REGISTRATION))
				|| IConst.VALUES.TRUE.equals(Configurator.getInstance().get(
						IConst.CONFIG.ENABLE_EMAIL_CONFIRMATION))) {
			return (mapping.findForward(IConst.TOKEN.DENIED));
		}

		HttpSession session = request.getSession();
		UserDAO dao = UserDAO.getInstance();

		try {

			if (dao.addUser((ProfileForm) form)) {
				User user = dao.getUser(((ProfileForm) form).getLogin(),
						((ProfileForm) form).getPassword());
				log(request, "logs.LOG7", user.getName());

				if (user.getStatus() > 0) {
					user.setIp(request.getRemoteAddr());
					session.setAttribute(IConst.SESSION.USER_KEY, user);

					if (IConst.VALUES.TRUE.equals(Configurator.getInstance()
							.get(IConst.CONFIG.ENABLE_AUTO_LOGIN))) {
						// set autolog cookies if needed...
						if (user.getSettings().isAutologin()) {
							Cookie userCookie = new Cookie(
									IConst.COOKIE.USER_COOKIE, user.getName()
											+ "*" + user.getPassword());
							userCookie
									.setMaxAge(IConst.COOKIE.SECONDS_PER_YEAR);
							((HttpServletResponse) response)
									.addCookie(userCookie);
						}
					}
				}
			} else {
				ActionMessages errors = new ActionMessages();

				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
						"errors.ERR6"));

				saveErrors(request, errors);

				return (mapping.getInputForward());
			}
		} catch (SQLException sqle) {
			getServlet().log("Connection.process", sqle);
			throw new SystemException(sqle);
		}

		return (mapping.findForward(IConst.TOKEN.WELCOME));
	}
}