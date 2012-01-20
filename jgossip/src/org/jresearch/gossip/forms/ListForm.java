/*
 * $Id: ListForm.java,v 1.3 2005/06/07 12:32:17 bel70 Exp $
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
 *              Dmitriy Belov <bel@jresearch.org>
 *               .
 * * ***** END LICENSE BLOCK ***** */
/*
 * Created on 18.05.2004
 *
 */
package org.jresearch.gossip.forms;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;
import org.jresearch.gossip.IConst;

/**
 * DOCUMENT ME!
 * 
 * @author Dmitry Belov
 */
public class ListForm extends ValidatorForm implements IConst {
	private String block = "0";

	/**
	 * Reset all properties to their default values.
	 * 
	 * @param mapping
	 *            The mapping used to select this instance
	 * @param request
	 *            The servlet request we are processing
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		this.block = "0";
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @return
	 */
	public String getBlock() {
		return block;
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @param string
	 */
	public void setBlock(String string) {
		block = string;
	}
}
