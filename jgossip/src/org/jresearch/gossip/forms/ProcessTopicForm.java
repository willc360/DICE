/*
 * $Id: ProcessTopicForm.java,v 1.3 2005/06/07 12:32:17 bel70 Exp $
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
 * Contributor(s): Dmitriy Belov <bel@jresearch.org> . * ***** END LICENSE BLOCK
 * *****
 */
/*
 * Created on 19.05.2004
 *  
 */
package org.jresearch.gossip.forms;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

/**
 * @author Dmitry Belov
 * 
 */
public class ProcessTopicForm extends ProcessForumForm {
	private String tid;

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		this.tid = null;
	}

	/**
	 * @return Returns the tid.
	 */
	public String getTid() {
		return tid;
	}

	/**
	 * @param tid
	 *            The tid to set.
	 */
	public void setTid(String mid) {
		this.tid = mid;
	}
}