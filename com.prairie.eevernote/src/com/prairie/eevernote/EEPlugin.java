package com.prairie.eevernote;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EEPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = Constants.PLUGIN_ID;

	// The shared instance
	private static EEPlugin plugin;

	/**
	 * The constructor
	 */
	public EEPlugin() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static EEPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static String getVersion() {
		return EEPlugin.getDefault().getBundle().getVersion().toString();
		// return ((BundleReference)EEClipperPlugin.class.getClassLoader()).getBundle().getVersion().toString();
		// return EEClipperPlugin.getDefault().getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
	}

	public static String getName() {
		return EEPlugin.getDefault().getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
	}

}
