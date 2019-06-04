package burp

import java.io.File
import javax.swing.JMenuItem


class BurpExtender: IBurpExtender {
    override fun registerExtenderCallbacks(callbacks: IBurpExtenderCallbacks) {
        callbacks.setExtensionName("Compare with Meld")
        callbacks.registerContextMenuFactory(ContextMenuFactory())
    }
}


class ContextMenuFactory: IContextMenuFactory {
    val requestContexts = listOf(IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST,
            IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST)
    val responseContexts = listOf(IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE,
            IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_RESPONSE)
    val allowedContexts = requestContexts + responseContexts

    var firstRequest: ByteArray? = null

    override fun createMenuItems(invocation: IContextMenuInvocation): List<JMenuItem> {
        if (allowedContexts.contains(invocation.invocationContext)) {
            val suffix = if (firstRequest == null) { "1" } else { "2" }
            val contextMenuItem = JMenuItem("Compare with Meld [$suffix]")
            contextMenuItem.addActionListener {
                val requestResponse = invocation.selectedMessages!![0]
                val data = if (requestContexts.contains(invocation.invocationContext)) {
                    requestResponse.request
                } else {
                    requestResponse.response
                }
                if (data == null) {
                    return@addActionListener
                }
                if (firstRequest == null) {
                    firstRequest = data
                    return@addActionListener
                }

                val firstFile = File.createTempFile("meld", "1")
                firstFile.writeBytes(firstRequest!!)
                val secondFile = File.createTempFile("meld", "2")
                secondFile.writeBytes(data)

                Runtime.getRuntime().exec(arrayOf(getMeldPath(), firstFile.absolutePath, secondFile.absolutePath))
                firstRequest = null
            }
            return listOf(contextMenuItem)
        }
        else {
            return emptyList()
        }
    }

    fun getMeldPath(): String {
        val osName = System.getProperty("os.name")
        if(osName.startsWith("Windows")) {
            return "c:/Program Files (x86)/Meld/Meld.exe"
        }
        else if(osName.equals("Mac OS X")) {
            return "/Applications/Meld.app/Contents/MacOS/Meld"
        }
        else if(osName.startsWith("Linux")) {
            return "/usr/bin/meld"
        }
        else {
            throw Exception("Unknown OS $osName")
        }
    }
}
