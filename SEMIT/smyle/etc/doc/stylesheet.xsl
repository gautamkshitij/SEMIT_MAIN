<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>
    
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="document">
    <html>
      <head><title>SMYLE: <xsl:value-of select="@title"/></title></head>
      <body  bgcolor="#ffffff" text="#000000">
        <table><tr><td valign="top">
         <table>
          <tr><td><a href="{@home}index.html"><img border="0" src="{@home}smyle_small.jpg" /></a></td></tr>
          <tr><td><a href="{@home}index.html">Home</a></td></tr>
          <tr><td><a href="http://sourceforge.net/project/showfiles.php?group_id=34021">Download!</a></td></tr>
          <tr><td><a href="{@home}features.html">Features</a></td></tr>
          <tr><td><a href="{@home}benchmark.html">Benchmark</a></td></tr>
          <tr><td><a href="{@home}users.html">Users</a></td></tr>
          <tr><td><a href="{@home}gettingstarted.html">Getting started</a></td></tr>
          <tr><td><a href="{@home}concepts.html">Concepts</a></td></tr>
          <tr><td><a href="{@home}data_types.html">Data types</a></td></tr>
          <tr><td><a href="{@home}javaapi/index.html">API (Java)</a></td></tr>
          <tr><td><a href="{@home}gjapi/index.html">API (GJ)</a></td></tr>
          <tr><td><a href="{@home}idioms.html">Idioms</a></td></tr>
          <tr><td><a href="{@home}architecture.html">Architecture</a></td></tr>
          <tr><td><a href="{@home}performance.html">Performance Tips</a></td></tr>
          <tr><td><a href="{@home}idl.html">IDL Grammar</a></td></tr>
          <tr><td><a href="{@home}browser.html">Browser</a></td></tr>
          <tr><td><a href="{@home}schema_evolution.html">Schema evolution</a></td></tr>
          <tr><td><a href="{@home}gj.html">GJ (Generic Java)</a></td></tr>
          <tr><td><a href="{@home}limitations.html">Limitations</a></td></tr>
          <tr><td><a href="{@home}history.html">History</a></td></tr>
          <tr><td><a href="http://sourceforge.net/forum/forum.php?forum_id=106587">Help Forum</a></td></tr>
         </table>
        </td><td width="20">
        </td><td valign="top">
        <h2>SMYLE: <xsl:value-of select="@title"/></h2>
        
        <xsl:apply-templates/>
          
        </td></tr>
        <tr><td colspan="3">
        <hr />
        <table width="100%"><tr>
        <td>
          Author: Stefan Reich (<a href="mailto:doc@drjava.de">doc@drjava.de</a>)<br />
          Smyle Homepage: <a href="http://www.drjava.de/smyle/">www.drjava.de/smyle</a><br />
        </td>
        <td align="right">
          <A href="http://sourceforge.net"><IMG src="http://sourceforge.net/sflogo.php?group_id=34021" width="88" height="31" border="0" alt="SourceForge Logo" /></A>
        </td></tr></table>
        </td></tr></table>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="h">
    <h3><xsl:apply-templates/></h3>
  </xsl:template>
  
  <xsl:template match="p">
    <p><xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="ul">
    <ul><xsl:apply-templates/></ul>
  </xsl:template>

  <xsl:template match="ol">
    <ol><xsl:apply-templates/></ol>
  </xsl:template>

  <xsl:template match="li">
    <li><xsl:apply-templates/></li>
  </xsl:template>

  <xsl:template match="codeblock">
    <pre><xsl:apply-templates/></pre>
  </xsl:template>
  
  <xsl:template match="code">
    <code><strong><xsl:apply-templates/></strong></code>
  </xsl:template>

  <xsl:template match="i">
    <i><xsl:apply-templates/></i>
  </xsl:template>
  
  <xsl:template match="em">
    <em><xsl:apply-templates/></em>
  </xsl:template>
  
  <xsl:template match="a">
    <a href="{@href}"><xsl:apply-templates/></a>
  </xsl:template>
  
  <xsl:template match="br">
    <br/>
  </xsl:template>
  
  <xsl:template match="faq">
    <dl><dd><dl>
      <xsl:apply-templates/>
    </dl></dd></dl>
  </xsl:template>
  
  <xsl:template match="question">
    <dt><xsl:apply-templates/><p/></dt>
  </xsl:template>
  
  <xsl:template match="answer">
    <dd><xsl:apply-templates/><p/></dd>
  </xsl:template>
  
  <xsl:template match="table">
    <table border="1"><xsl:apply-templates/></table>
  </xsl:template>

  <xsl:template match="tr">
    <tr><xsl:apply-templates/></tr>
  </xsl:template>

  <xsl:template match="th">
    <th><xsl:apply-templates/></th>
  </xsl:template>

  <xsl:template match="td">
    <td><xsl:apply-templates/></td>
  </xsl:template>
  
  <xsl:template match="img">
    <img src="{@src}" />
  </xsl:template>
  
  <xsl:template match="logo">
    <img src="smyle_blue.jpg" align="right" />
  </xsl:template>
  
  <xsl:template match="logos">
    <a href="http://www.jedit.org"><img src="http://www.jedit.org/made-with-jedit-1.gif"
     alt="Made with jEdit" border="0" width="131" height="44" /></a>
  </xsl:template>
</xsl:stylesheet>
