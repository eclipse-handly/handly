/*******************************************************************************
 * Copyright (c) 2015 Codasip Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ondrej Ilcik (Codasip) - initial implementation
 *******************************************************************************/
package org.eclipse.handly.internal.examples.javamodel.ui.navigator;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME =
        "org.eclipse.handly.internal.examples.javamodel.ui.navigator.messages"; //$NON-NLS-1$
    public static String NavigatorLabelProvider_DefaultPackage;
    public static String OpenAction_Label;
    public static String OpenActionProvider_Label;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
