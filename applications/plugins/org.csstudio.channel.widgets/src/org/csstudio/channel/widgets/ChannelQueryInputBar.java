package org.csstudio.channel.widgets;

import gov.bnl.channelfinder.api.ChannelQuery;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.csstudio.ui.util.helpers.ComboHistoryHelper;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

public class ChannelQueryInputBar extends AbstractChannelQueryWidget 
	implements ISelectionProvider {

	private Combo combo;
	
	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public ChannelQueryInputBar(Composite parent, int style,
			IDialogSettings dialogSettings, String settingsKey) {
		super(parent, style);
		setLayout(new FillLayout(SWT.HORIZONTAL));

		ComboViewer comboViewer = new ComboViewer(this, SWT.NONE);
		combo = comboViewer.getCombo();
		combo.setToolTipText(
				"space seperated search criterias, patterns may include * and ? wildcards\r\nchannelNamePatter\r\npropertyName=propertyValuePattern1,propertyValuePattern2\r\nTags=tagNamePattern\r\nEach criteria is logically ANDed and , or || seperated values are logically ORed\r\n");

		ComboHistoryHelper name_helper = new ComboHistoryHelper(dialogSettings,
				settingsKey, combo, 20, true) {
			@Override
			public void newSelection(final String queryText) {
				setChannelQuery(ChannelQuery.Builder.query(queryText).create());
			}
		};
		
		addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if ("channelQuery".equals(event.getPropertyName())) {
					String newValue = "";
					if (event.getNewValue() != null)
						newValue = ((ChannelQuery) event.getNewValue()).getQuery();
					if (!newValue.equals(combo.getText())) {
						combo.setText(newValue);
					}
				}
			}
		});
		
		selectionProvider = new AbstractSelectionProviderWrapper(comboViewer, this) {
			
			@Override
			protected ISelection transform(IStructuredSelection selection) {
				return new StructuredSelection(getChannelQuery());
			}
		};
		
		name_helper.loadSettings();
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
	
	@Override
	public void setMenu(Menu menu) {
		super.setMenu(menu);
		combo.setMenu(menu);
	}
	
	private AbstractSelectionProviderWrapper selectionProvider;

	@Override
	public void addSelectionChangedListener(final ISelectionChangedListener listener) {
		selectionProvider.addSelectionChangedListener(listener);
	}

	@Override
	public ISelection getSelection() {
		return selectionProvider.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionProvider.removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		selectionProvider.setSelection(selection);
	}
	
}
