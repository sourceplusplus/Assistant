package com.sourceplusplus.plugin.intellij.portal

import com.google.common.base.Joiner
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.sourceplusplus.portal.display.PortalTab
import com.sourceplusplus.portal.display.PortalUI
import io.netty.handler.codec.http.QueryStringDecoder
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandler

import javax.swing.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * Used to display the Source++ Portal UI.
 *
 * @version 0.3.1
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJPortalUI extends PortalUI implements CefLifeSpanHandler {

    private JBCefBrowser parentBrowser
    private JBCefBrowser browser
    private Map<String, String> currentQueryParams = [:]
    private static boolean DARK_MODE

    IntelliJPortalUI(String portalUuid, JBCefBrowser browser) {
        super(portalUuid)
        this.browser = browser
        if (browser != null) {
            browser.JBCefClient.addLifeSpanHandler(this, browser.cefBrowser)
        }
    }

    void lateInitBrowser(JBCefBrowser browser) {
        this.browser = Objects.requireNonNull(browser)
        browser.JBCefClient.addLifeSpanHandler(this, browser.cefBrowser)
    }

    void cloneUI(IntelliJPortalUI portalUI) {
        parentBrowser = portalUI.parentBrowser
        super.cloneUI(portalUI)
    }

    void loadPage(PortalTab tab) {
        loadPage(tab, [:])
    }

    void loadPage(PortalTab tab, Map<String, String> queryParams) {
        queryParams.put("dark_mode", Boolean.toString(DARK_MODE))
        currentQueryParams = new HashMap<>(queryParams)
        def userQuery = Joiner.on("&").withKeyValueSeparator("=").join(queryParams)
        if (userQuery) {
            userQuery = "&$userQuery"
        }
        browser.loadURL(getPortalUrl(tab, portalUuid, userQuery))
    }

    void close() {
        browser.getJBCefClient().removeLifeSpanHandler(this, browser.cefBrowser)
        Disposer.dispose(browser)
    }

    void reload() {
        if (browser != null) {
            loadPage(currentTab, currentQueryParams)
        }
    }

    static void updateTheme(boolean dark) {
        DARK_MODE = dark
        IntelliJSourcePortal.getPortals().each {
            it.portalUI.reload()
        }
    }

    @Override
    boolean onBeforePopup(CefBrowser browser, CefFrame frame, String targetUrl, String targetFrameName) {
        def portal = IntelliJSourcePortal.getPortal(new QueryStringDecoder(
                targetUrl).parameters().get("portal_uuid").get(0))
        if (portal.portalUI.parentBrowser == null) {
            portal.portalUI.parentBrowser = JBCefBrowser.getJBCefBrowser(browser)
        }
        def popupBrowser = browser.client.createBrowser(targetUrl, false, false)
        if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            popupBrowser.createImmediately()
        }
        portal.portalUI.browser = new JBCefBrowser(popupBrowser, this.browser.JBCefClient, false, targetUrl)
        portal.portalUI.parentBrowser.JBCefClient.addLifeSpanHandler(this, portal.portalUI.browser.cefBrowser)

        def popupFrame = new JFrame(portal.portalUI.viewingPortalArtifact)
        popupFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        popupFrame.setPreferredSize(new Dimension(800, 600))
        popupFrame.add(portal.portalUI.browser.component)
        popupFrame.pack()
        popupFrame.setLocationByPlatform(true)
        popupFrame.setVisible(true)
        popupFrame.addWindowListener(new WindowAdapter() {
            @Override
            void windowClosing(WindowEvent e) {
                portal.close()
            }
        })

        return true
    }

    @Override
    void onAfterCreated(CefBrowser cefBrowser) {
    }

    @Override
    void onAfterParentChanged(CefBrowser cefBrowser) {
    }

    @Override
    boolean doClose(CefBrowser cefBrowser) {
        return false
    }

    @Override
    void onBeforeClose(CefBrowser cefBrowser) {
    }
}
