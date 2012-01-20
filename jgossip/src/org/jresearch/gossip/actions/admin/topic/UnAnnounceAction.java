/*
 * $$Id: UnAnnounceAction.java,v 1.3 2005/06/07 12:31:53 bel70 Exp $$
 * 
 * ***** BEGIN LICENSE BLOCK ***** The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is JGossip forum code.
 * 
 * The Initial Developer of the Original Code is the JResearch, Org. Portions
 * created by the Initial Developer are Copyright (C) 2004 the Initial
 * Developer. All Rights Reserved.
 * 
 * Contributor(s): Dmitry Belov <bel@jresearch.org>
 * 
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Sep 27, 2003
 *  
 */
package org.jresearch.gossip.actions.admin.topic;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.jresearch.gossip.IConst;
import org.jresearch.gossip.actions.BaseAction;
import org.jresearch.gossip.beans.user.User;
import org.jresearch.gossip.dao.ForumDAO;
import org.jresearch.gossip.exception.SystemException;
import org.jresearch.gossip.forms.ProcessTopicForm;

/**
 * DOCUMENT ME!
 * 
 * @author Bel
 */
public class UnAnnounceAction extends BaseAction {
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
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute(IConst.SESSION.USER_KEY);
		ProcessTopicForm ptForm = (ProcessTopicForm) form;
		ForumDAO dao = ForumDAO.getInstance();
		try {
			if (!dao.checkMod(Integer.parseInt(ptForm.getFid()), user)) {
				return (mapping.findForward(IConst.TOKEN.DENIED));
			}

			dao.setThreadSortBy(ptForm.getTid(), 9);
			log(request, "logs.LOG21", ptForm.getTid());
		} catch (SQLException sqle) {
			getServlet().log("Connection.process", sqle);
			throw new SystemException(sqle);
		}
		return (new ActionForward("/ShowForum.do?fid=" + ptForm.getFid(), true));
	}
}