/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.ui;

import static com.formdev.flatlaf.util.UIScale.scale;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.HiDPIUtils;
import com.formdev.flatlaf.util.JavaCompatibility;
import com.formdev.flatlaf.util.UIScale;

/**
 * Provides the Flat LaF UI delegate for {@link javax.swing.JTextField}.
 *
 * <!-- BasicTextFieldUI -->
 *
 * @uiDefault TextField.font					Font
 * @uiDefault TextField.background				Color
 * @uiDefault TextField.foreground				Color	also used if not editable
 * @uiDefault TextField.caretForeground			Color
 * @uiDefault TextField.selectionBackground		Color
 * @uiDefault TextField.selectionForeground		Color
 * @uiDefault TextField.disabledBackground		Color	used if not enabled
 * @uiDefault TextField.inactiveBackground		Color	used if not editable
 * @uiDefault TextField.inactiveForeground		Color	used if not enabled (yes, this is confusing; this should be named disabledForeground)
 * @uiDefault TextField.border					Border
 * @uiDefault TextField.margin					Insets
 * @uiDefault TextField.caretBlinkRate			int		default is 500 milliseconds
 *
 * <!-- FlatTextFieldUI -->
 *
 * @uiDefault Component.minimumWidth			int
 * @uiDefault Component.isIntelliJTheme			boolean
 * @uiDefault TextField.placeholderForeground	Color
 * @uiDefault TextField.focusedBackground		Color	optional
 * @uiDefault TextComponent.selectAllOnFocusPolicy	String	never, once (default) or always
 * @uiDefault TextComponent.selectAllOnMouseClick	boolean
 *
 * @author Karl Tauber
 */
public class FlatTextFieldUI
	extends BasicTextFieldUI
{
	protected int minimumWidth;
	protected boolean isIntelliJTheme;
	protected Color placeholderForeground;
	protected Color focusedBackground;

	private FocusListener focusListener;

	public static ComponentUI createUI( JComponent c ) {
		return new FlatTextFieldUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();

		String prefix = getPropertyPrefix();
		minimumWidth = UIManager.getInt( "Component.minimumWidth" );
		isIntelliJTheme = UIManager.getBoolean( "Component.isIntelliJTheme" );
		placeholderForeground = UIManager.getColor( prefix + ".placeholderForeground" );
		focusedBackground = UIManager.getColor( prefix + ".focusedBackground" );

		LookAndFeel.installProperty( getComponent(), "opaque", false );

		MigLayoutVisualPadding.install( getComponent() );
	}

	@Override
	protected void uninstallDefaults() {
		super.uninstallDefaults();

		placeholderForeground = null;
		focusedBackground = null;

		MigLayoutVisualPadding.uninstall( getComponent() );
	}

	@Override
	protected void installListeners() {
		super.installListeners();

		// necessary to update focus border and background
		focusListener = new FlatUIUtils.RepaintFocusListener( getComponent(), null );
		getComponent().addFocusListener( focusListener );
	}

	@Override
	protected void uninstallListeners() {
		super.uninstallListeners();

		getComponent().removeFocusListener( focusListener );
		focusListener = null;
	}

	@Override
	protected Caret createCaret() {
		return new FlatCaret( UIManager.getString( "TextComponent.selectAllOnFocusPolicy"),
			UIManager.getBoolean( "TextComponent.selectAllOnMouseClick" ) );
	}

	@Override
	protected void propertyChange( PropertyChangeEvent e ) {
		super.propertyChange( e );
		propertyChange( getComponent(), e );
	}

	static void propertyChange( JTextComponent c, PropertyChangeEvent e ) {
		switch( e.getPropertyName() ) {
			case FlatClientProperties.PLACEHOLDER_TEXT:
			case FlatClientProperties.COMPONENT_ROUND_RECT:
			case FlatClientProperties.TEXT_FIELD_PADDING:
				c.repaint();
				break;

			case FlatClientProperties.MINIMUM_WIDTH:
				c.revalidate();
				break;
		}
	}

	@Override
	protected void paintSafely( Graphics g ) {
		paintBackground( g, getComponent(), isIntelliJTheme, focusedBackground );
		paintPlaceholder( g, getComponent(), placeholderForeground );

		super.paintSafely( HiDPIUtils.createGraphicsTextYCorrection( (Graphics2D) g ) );
	}

	@Override
	protected void paintBackground( Graphics g ) {
		// background is painted elsewhere
	}

	static void paintBackground( Graphics g, JTextComponent c, boolean isIntelliJTheme, Color focusedBackground ) {
		// do not paint background if:
		//   - not opaque and
		//   - border is not a flat border and
		//   - opaque was explicitly set (to false)
		// (same behaviour as in AquaTextFieldUI)
		if( !c.isOpaque() && FlatUIUtils.getOutsideFlatBorder( c ) == null && FlatUIUtils.hasOpaqueBeenExplicitlySet( c ) )
			return;

		float focusWidth = FlatUIUtils.getBorderFocusWidth( c );
		float arc = FlatUIUtils.getBorderArc( c );

		// fill background if opaque to avoid garbage if user sets opaque to true
		if( c.isOpaque() && (focusWidth > 0 || arc > 0) )
			FlatUIUtils.paintParentBackground( g, c );

		// paint background
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			FlatUIUtils.setRenderingHints( g2 );

			g2.setColor( getBackground( c, isIntelliJTheme, focusedBackground ) );
			FlatUIUtils.paintComponentBackground( g2, 0, 0, c.getWidth(), c.getHeight(), focusWidth, arc );
		} finally {
			g2.dispose();
		}
	}

	static Color getBackground( JTextComponent c, boolean isIntelliJTheme, Color focusedBackground ) {
		Color background = c.getBackground();

		// always use explicitly set color
		if( !(background instanceof UIResource) )
			return background;

		// focused
		if( focusedBackground != null && FlatUIUtils.isPermanentFocusOwner( c ) )
			return focusedBackground;

		// for compatibility with IntelliJ themes
		if( isIntelliJTheme && (!c.isEnabled() || !c.isEditable()) )
			return FlatUIUtils.getParentBackground( c );

		return background;
	}

	static void paintPlaceholder( Graphics g, JTextComponent c, Color placeholderForeground ) {
		// check whether text component is empty
		if( c.getDocument().getLength() > 0 )
			return;

		// check for JComboBox
		Container parent = c.getParent();
		JComponent jc = (parent instanceof JComboBox) ? (JComboBox<?>) parent : c;

		// get placeholder text
		Object placeholder = jc.getClientProperty( FlatClientProperties.PLACEHOLDER_TEXT );
		if( !(placeholder instanceof String) )
			return;

		// compute placeholder location
		Insets insets = c.getInsets();
		FontMetrics fm = c.getFontMetrics( c.getFont() );
		int x = insets.left;
		int y = insets.top + fm.getAscent() + ((c.getHeight() - insets.top - insets.bottom - fm.getHeight()) / 2);

		// paint placeholder
		g.setColor( placeholderForeground );
		String clippedPlaceholder = JavaCompatibility.getClippedString( jc, fm,
				(String) placeholder, c.getWidth() - insets.left - insets.right );
		FlatUIUtils.drawString( c, g, clippedPlaceholder, x, y );
	}

	@Override
	public Dimension getPreferredSize( JComponent c ) {
		return applyMinimumWidth( c, super.getPreferredSize( c ), minimumWidth );
	}

	@Override
	public Dimension getMinimumSize( JComponent c ) {
		return applyMinimumWidth( c, super.getMinimumSize( c ), minimumWidth );
	}

	static Dimension applyMinimumWidth( JComponent c, Dimension size, int minimumWidth ) {
		// do not apply minimum width if JTextField.columns is set
		if( c instanceof JTextField && ((JTextField)c).getColumns() > 0 )
			return size;

		// do not apply minimum width if used in combobox or spinner
		Container parent = c.getParent();
		if( parent instanceof JComboBox ||
			parent instanceof JSpinner ||
			(parent != null && parent.getParent() instanceof JSpinner) )
		  return size;

		minimumWidth = FlatUIUtils.minimumWidth( c, minimumWidth );
		float focusWidth = FlatUIUtils.getBorderFocusWidth( c );
		size.width = Math.max( size.width, scale( minimumWidth ) + Math.round( focusWidth * 2 ) );
		return size;
	}

	@Override
	protected Rectangle getVisibleEditorRect() {
		Rectangle r = super.getVisibleEditorRect();
		if( r != null ) {
			// remove padding
			Insets padding = getPadding();
			if( padding != null ) {
				r = FlatUIUtils.subtractInsets( r, padding );
				r.width = Math.max( r.width, 0 );
				r.height = Math.max( r.height, 0 );
			}
		}
		return r;
	}

	/**
	 * @since 1.4
	 */
	protected Insets getPadding() {
		Object padding = getComponent().getClientProperty( FlatClientProperties.TEXT_FIELD_PADDING );
		return (padding instanceof Insets) ? UIScale.scale( (Insets) padding ) : null;
	}
}
