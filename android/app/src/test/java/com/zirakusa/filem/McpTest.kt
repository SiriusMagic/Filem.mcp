package com.zirakusa.filem

import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.mcp.Access
import com.zirakusa.filem.mcp.HubLink
import com.zirakusa.filem.mcp.McpHandler
import com.zirakusa.filem.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Filem's MCP: tools list, good calls, bad arguments, not found, read-only, paths that try to escape, hub link. */
@RunWith(RobolectricTestRunner::class)
class McpTest {
    @get:Rule val tmp = TemporaryFolder()
    private val root by lazy { tmp.newFolder("storage").also {
        File(it, "Download").mkdirs(); File(it, "Download/informe.txt").writeText("Hola Filem"); File(it, "DCIM").mkdirs()
    } }
    private val reader = Token("fil_" + "r".repeat(40), Access.READ)
    private val writer = Token("fil_" + "w".repeat(40), Access.WRITE)
    private var changes = 0
    private val handler by lazy { McpHandler({ listOf(reader, writer) }, { FileStore(root) }, "0.1.0", onChanged = { changes++ }) }

    private fun raw(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString())
    }
    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = JSONObject(raw(token, method, params).body).getJSONObject("result")
    private fun tool(token: Token, name: String, args: JSONObject) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(token: Token, name: String, args: JSONObject = JSONObject()) =
        tool(token, name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(token: Token, name: String, args: JSONObject = JSONObject()) = tool(token, name, args).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")

    @Test fun toolsFollowTheAccess() {
        fun names(t: Token) = rpc(t, "tools/list").getJSONArray("tools").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("name") } }
        assertEquals(listOf("files_list", "files_search", "files_info", "files_recent", "files_storage", "files_read_text"), names(reader))
        assertTrue(names(writer).containsAll(listOf("files_create_folder", "files_rename", "files_copy", "files_move", "files_trash")))
        assertEquals("filem", rpc(reader, "initialize").getJSONObject("serverInfo").getString("name"))
    }

    @Test fun goodCalls() {
        assertEquals(2, ok(reader, "files_list").getInt("total"))
        assertEquals("Hola Filem", ok(reader, "files_read_text", JSONObject().put("path", "Download/informe.txt")).getString("text"))
        assertEquals("Download/informe.txt", ok(reader, "files_search", JSONObject().put("query", "INFORME")).getJSONArray("files")
            .getJSONObject(0).getString("path"))
        File(root, "Download/fotos.zip").writeText("zip")
        assertEquals("zips", ok(reader, "files_search", JSONObject().put("category", "zips")).getJSONArray("files")
            .getJSONObject(0).getString("category"))
        assertEquals(2, ok(reader, "files_search", JSONObject().put("category", "received")).getJSONArray("files").length())
        val storage = ok(reader, "files_storage").getJSONObject("categories")
        assertEquals(1, storage.getJSONObject("zips").getInt("files"))
        assertEquals(2, storage.getJSONObject("downloads").getInt("files"))
        assertEquals("folder", ok(writer, "files_create_folder", JSONObject().put("path", "DCIM").put("name", "Viaje")).getString("kind"))
        ok(writer, "files_copy", JSONObject().put("paths", JSONArray().put("Download/informe.txt")).put("to", "DCIM/Viaje"))
        assertTrue(File(root, "DCIM/Viaje/informe.txt").exists())
        assertTrue(ok(writer, "files_trash", JSONObject().put("path", "Download/informe.txt")).getBoolean("restorable"))
        assertFalse(File(root, "Download/informe.txt").exists())
        assertEquals(3, changes)
    }

    @Test fun badArgumentsAndNotFound() {
        assertEquals("BAD_ARGUMENT", code(reader, "files_list", JSONObject().put("sort", "color")))
        assertEquals("BAD_ARGUMENT", code(reader, "files_list", JSONObject().put("extra", 1)))
        assertEquals("BAD_ARGUMENT", code(reader, "files_search", JSONObject()))
        assertEquals("BAD_ARGUMENT", code(reader, "files_search", JSONObject().put("category", "rars")))
        assertEquals("BAD_ARGUMENT", code(writer, "files_move", JSONObject().put("paths", "Download").put("to", "DCIM")))
        assertEquals("BAD_NAME", code(writer, "files_rename", JSONObject().put("path", "DCIM").put("new_name", "../x")))
        assertEquals("NOT_FOUND", code(reader, "files_info", JSONObject().put("path", "nada/aqui.txt")))
        assertEquals("NOT_FOUND", code(reader, "files_search", JSONObject().put("category", "zips").put("under", "nada")))
        assertEquals("BAD_PATH", code(reader, "files_list", JSONObject().put("path", "../")))
        assertEquals("BAD_PATH", code(reader, "files_list", JSONObject().put("path", ".filem")))
        assertEquals("UNKNOWN_TOOL", code(reader, "files_format"))
    }

    @Test fun readOnlyPausedAndWrongTokens() {
        assertEquals("READ_ONLY", code(reader, "files_trash", JSONObject().put("path", "DCIM")))
        assertTrue(File(root, "DCIM").exists())
        assertEquals(401, raw(Token("wrong_" + "x".repeat(40)), "tools/list").status)
        assertEquals(401, runBlocking { handler.handle("POST", "/mcp", emptyMap(), emptyMap(), "{}") }.status)
    }

    @Test fun hubLinkNeedsTheSameDeveloper() {
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.connect(false).getString("error"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.tool(false, "{}").getString("error"))
        // Only the same developer (Arka) may connect, disconnect, read the status or run a tool.
        listOf(HubLink.disconnect(false), HubLink.status(false)).forEach { assertEquals("NOT_SAME_DEVELOPER", it.getString("error")) }
    }

    /** Lucía runs a tool on the phone through the same handler, with or without the public link; Arka turns that on and off. */
    @Test fun luciaRunsToolsOnThePhone() {
        val relay = com.zirakusa.filem.mcp.McpMode.relayUrl
        com.zirakusa.filem.mcp.McpMode.relayUrl = ""                                             // no relay in tests
        com.zirakusa.filem.mcp.McpMode.load(org.robolectric.RuntimeEnvironment.getApplication())
        com.zirakusa.filem.mcp.McpMode.turnRemote(false)
        val list = JSONObject(com.zirakusa.filem.mcp.McpMode.local("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")!!)
        assertTrue(list.getJSONObject("result").getJSONArray("tools").length() > 0)
        val viaProvider = JSONObject(HubLink.tool(true, """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""").getString("response"))
        assertEquals(2, viaProvider.getInt("id"))
        assertEquals("BAD_REQUEST", HubLink.tool(true, "").getString("error"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.tool(false, "{}").getString("error"))
        assertTrue(HubLink.status(true).containsKey("remote"))
        HubLink.connect(true); assertTrue(com.zirakusa.filem.mcp.McpMode.remote)                 // Arka › «Activar MCP»
        assertFalse(HubLink.disconnect(true).getBoolean("remote")); assertFalse(com.zirakusa.filem.mcp.McpMode.remote)
        com.zirakusa.filem.mcp.McpMode.relayUrl = relay                                          // as the other tests expect
    }

    /** The hub's token from before Arka took over is kept, so the hub's link keeps working. */
    @Test fun theOldHubTokenIsKept() {
        val old = """[{"id":"a","secret":"${"l".repeat(44)}","kind":"llm"},{"id":"b","secret":"${"h".repeat(44)}","kind":"redirect"}]"""
        assertEquals("h".repeat(44), com.zirakusa.filem.mcp.Legacy.redirectSecret(old))
        assertEquals(null, com.zirakusa.filem.mcp.Legacy.redirectSecret("[]"))
        assertEquals(null, com.zirakusa.filem.mcp.Legacy.redirectSecret(""))
    }
}
