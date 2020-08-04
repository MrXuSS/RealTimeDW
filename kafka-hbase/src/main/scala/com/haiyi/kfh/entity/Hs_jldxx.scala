package com.haiyi.kfh.entity

/**
 * @Author:XuChengLei
 * @Date:2020-07-06
 *
 */
case class Hs_jldxx(
                      GZDBH:String,
                      JLDBH:String,
                      YWLBDM:String,
                      JLDXH:String,
                      CBJHBH:String,
                      CBQDBH:String,
                      CBZQ:String,
                      JLDCBSXH:String,
                      DFNY:String,
                      BQCBCS:String,
                      YCBCS:String,
                      JSLXDM:String,
                      CZCS:String,
                      CZNY:String,
                      JSHH:String,
                      YHBH:String,
                      YHMC:String,
                      YDDZ:String,
                      YHZTDM:String,
                      DJBBBH:String,
                      JLDYTDM:String,
                      JLFSDM:String,
                      JLDYDJDM:String,
                      XLXDBS:String,
                      TQBS:String,
                      WYJJSFSDM:String,
                      YHWYJQSR:String,
                      DJCLBH:String,
                      YGDDL:String,
                      DYBH:String,
                      DYZH:String,
                      DLJSFSDM:String,
                      DLDBZ:String,
                      DBKJBZ:String,
                      DLFTGS:String,
                      BSFTFSDM:String,
                      YGBSXYZ:String,
                      WGBSXYZ:String,
                      BSJFBZ:String,
                      XSJSFSDM:String,
                      YGXSJSZ:String,
                      WGXSJSZ:String,
                      XSJFBZ:String,
                      XSFTBZ:String,
                      XSFTXYZ:String,
                      YDRL:String,
                      PTBBDM:String,
                      CTBBDM:String,
                      YDLBDM:String,
                      DJDM:String,
                      DJ:String,
                      GLYSBZDM:String,
                      HYFLDM:String,
                      FSJFBZ:String,
                      JBDFJSFSDM:String,
                      XLHDZ:String,
                      GDLL:String,
                      JBDFFTFS:String,
                      JBDFFTZ:String,
                      LTDFJSFS:String,
                      JTDLBL:String,
                      ZJSL:String,
                      FGZS:String,
                      SJLX:String,
                      YGZDBH:String,
                      GLYSKHFSDM:String,
                      JFYXJ:String,
                      GDDWBM:String,
                      JSBZ:String,
                      HSZTDM:String,
                      SCCBRQ:String,
                      CBRQ:String,
                      YDTS:String,
                      CYTZDF:String,
                      YGFBKJDL:String,
                      YGBSDL:String,
                      YGXSDL:String,
                      YGFTDL:String,
                      YGTBDL:String,
                      YGZDL:String,
                      WGFBKJDL:String,
                      WGBSDL:String,
                      WGXSDL:String,
                      WGFTDL:String,
                      WGTBDL:String,
                      KFRQ:String,
                      JSRQ:String,
                      FXRQ:String,
                      JTLX:String,
                      YGCJDL:String,
                      YGHBDL:String,
                      WGCJDL:String,
                      WGHBDL:String,
                      NLJDL:String,
                      MFDL:String,
                      TBDF:String,
                      YSDF:String,
                      WGZDL:String,
                      ZJRL:String,
                      JFRL:String,
                      JFXL:String,
                      JBFDJ:String,
                      SJLL:String,
                      TZXS:String,
                      ZBJLDBH:String,
                      KJXBDL:String,
                      JFDL:String,
                      DDDF:String,
                      JBDF:String,
                      LTDF:String,
                      FJFHJ:String,
                      ZDF:String,
                      PJLXDM:String,
                      YHLBDM:String,
                      CXDM:String,
                      YXBZ:String,
                      CZRBS:String,
                      CZSJ:String,
                      DWBM:String,
                      DQBM:String,
                      CJSJ:String,
                      SCDL:String,
                      SCGZDBH:String,
                      CDZXLDF:String,
                      GTSYDL:String,
                      CJSBZ:String,
                      GLBH:String,
                      JTNF:String,
                      BZ:String,
                      TCDM:String,
                      TCLJDF:String,
                      TCKSSJ:String,
                      TCJSSJ:String,
                      TBGZDBH:String,
                      JLDZTDM:String,
                      HCRQ:String
                   ){

  def get(str:String): String ={
    val result: String = str match {
      case "GZDBH" => this.GZDBH
      case "JLDBH" => this.JLDBH
      case "YWLBDM" => this.YWLBDM
      case "JLDXH" => this.JLDXH
      case "CBJHBH" => this.CBJHBH
      case "CBQDBH" => this.CBQDBH
      case "CBZQ" => this.CBZQ
      case "JLDCBSXH" => this.JLDCBSXH
      case "DFNY" => this.DFNY
      case "BQCBCS" => this.BQCBCS
      case "YCBCS" => this.YCBCS
      case "JSLXDM" => this.JSLXDM
      case "CZCS" => this.CZCS
      case "CZNY" => this.CZNY
      case "JSHH" => this.JSHH
      case "YHBH" => this.YHBH
      case "YHMC" => this.YHMC
      case "YDDZ" => this.YDDZ
      case "YHZTDM" => this.YHZTDM
      case "DJBBBH" => this.DJBBBH
      case "JLDYTDM" => this.JLDYTDM
      case "JLFSDM" => this.JLFSDM
      case "JLDYDJDM" => this.JLDYDJDM
      case "XLXDBS" => this.XLXDBS
      case "TQBS" => this.TQBS
      case "WYJJSFSDM" => this.WYJJSFSDM
      case "YHWYJQSR" => this.YHWYJQSR
      case "DJCLBH" => this.DJCLBH
      case "YGDDL" => this.YGDDL
      case "DYBH" => this.DYBH
      case "DYZH" => this.DYZH
      case "DLJSFSDM" => this.DLJSFSDM
      case "DLDBZ" => this.DLDBZ
      case "DBKJBZ" => this.DBKJBZ
      case "DLFTGS" => this.DLFTGS
      case "BSFTFSDM" => this.BSFTFSDM
      case "YGBSXYZ" => this.YGBSXYZ
      case "WGBSXYZ" => this.WGBSXYZ
      case "BSJFBZ" => this.BSJFBZ
      case "XSJSFSDM" => this.XSJSFSDM
      case "YGXSJSZ" => this.YGXSJSZ
      case "WGXSJSZ" => this.WGXSJSZ
      case "XSJFBZ" => this.XSJFBZ
      case "XSFTBZ" => this.XSFTBZ
      case "XSFTXYZ" => this.XSFTXYZ
      case "YDRL" => this.YDRL
      case "PTBBDM" => this.PTBBDM
      case "CTBBDM" => this.CTBBDM
      case "YDLBDM" => this.YDLBDM
      case "DJDM" => this.DJDM
      case "DJ" => this.DJ
      case "GLYSBZDM" => this.GLYSBZDM
      case "HYFLDM" => this.HYFLDM
      case "FSJFBZ" => this.FSJFBZ
      case "JBDFJSFSDM" => this.JBDFJSFSDM
      case "XLHDZ" => this.XLHDZ
      case "GDLL" => this.GDLL
      case "JBDFFTFS" => this.JBDFFTFS
      case "JBDFFTZ" => this.JBDFFTZ
      case "LTDFJSFS" => this.LTDFJSFS
      case "JTDLBL" => this.JTDLBL
      case "ZJSL" => this.ZJSL
      case "FGZS" => this.FGZS
      case "SJLX" => this.SJLX
      case "YGZDBH" => this.YGZDBH
      case "GLYSKHFSDM" => this.GLYSKHFSDM
      case "JFYXJ" => this.JFYXJ
      case "GDDWBM" => this.GDDWBM
      case "JSBZ" => this.JSBZ
      case "HSZTDM" => this.HSZTDM
      case "SCCBRQ" => this.SCCBRQ
      case "CBRQ" => this.CBRQ
      case "YDTS" => this.YDTS
      case "CYTZDF" => this.CYTZDF
      case "YGFBKJDL" => this.YGFBKJDL
      case "YGBSDL" => this.YGBSDL
      case "YGXSDL" => this.YGXSDL
      case "YGFTDL" => this.YGFTDL
      case "YGTBDL" => this.YGTBDL
      case "YGZDL" => this.YGZDL
      case "WGFBKJDL" => this.WGFBKJDL
      case "WGBSDL" => this.WGBSDL
      case "WGXSDL" => this.WGXSDL
      case "WGFTDL" => this.WGFTDL
      case "WGTBDL" => this.WGTBDL
      case "KFRQ" => this.KFRQ
      case "JSRQ" => this.JSRQ
      case "FXRQ" => this.FXRQ
      case "JTLX" => this.JTLX
      case "YGCJDL" => this.YGHBDL
      case "YGHBDL" => this.YGHBDL
      case "WGCJDL" => this.WGCJDL
      case "WGHBDL" => this.WGHBDL
      case "NLJDL" => this.NLJDL
      case "MFDL" => this.MFDL
      case "TBDF" => this.TBDF
      case "YSDF" => this.YSDF
      case "WGZDL" => this.WGZDL
      case "ZJRL" => this.ZJRL
      case "JFRL" => this.JFRL
      case "JFXL" => this.JFXL
      case "JBFDJ" => this.JBFDJ
      case "SJLL" => this.SJLL
      case "TZXS" => this.TZXS
      case "ZBJLDBH" => this.ZBJLDBH
      case "KJXBDL" => this.KJXBDL
      case "JFDL" => this.JFDL
      case "DDDF" => this.DDDF
      case "JBDF" => this.JBDF
      case "LTDF" => this.LTDF
      case "FJFHJ" => this.FJFHJ
      case "ZDF" => this.ZDF
      case "PJLXDM" => this.PJLXDM
      case "YHLBDM" => this.YHLBDM
      case "CXDM" => this.CXDM
      case "YXBZ" => this.YXBZ
      case "CZRBS" => this.CZRBS
      case "CZSJ" => this.CZSJ
      case "DWBM" => this.DWBM
      case "DQBM" => this.DQBM
      case "CJSJ" => this.CJSJ
      case "SCDL" => this.SCDL
      case "SCGZDBH" => this.SCGZDBH
      case "CDZXLDF" => this.CDZXLDF
      case "GTSYDL" => this.GTSYDL
      case "CJSBZ" => this.CJSBZ
      case "GLBH" => this.GLBH
      case "JTNF" => this.JTNF
      case "BZ" => this.BZ
      case "TCDM" => this.TCDM
      case "TCLJDF" => this.TCLJDF
      case "TCKSSJ" => this.TCKSSJ
      case "TCJSSJ" => this.TCJSSJ
      case "TBGZDBH" => this.TBGZDBH
      case "JLDZTDM" => this.JLDZTDM
      case "HCRQ" => this.HCRQ
      case _ => "not found"
    }
    result
  }
}
