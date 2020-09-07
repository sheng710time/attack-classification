package com.ditecting.attackclassification.anomalyclassification.showdata;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.LengthAdjustmentType;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.DefaultXYDataset;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.util.*;
import java.util.List;


public class ShowJFreeChart {
	
	/**
	 * 绘制dp聚类结果
	 * 
	 */
	public static void show2DScatterPlot_dp(List<Map.Entry<Integer, Double>> sortedGammaList) {
		Map scatterPlotClu = ShowDataHandler.dpTo2DScatterPlot(sortedGammaList);
		//获取ac_cnlList
		LinkedList ac_cnlList = (LinkedList) scatterPlotClu.get("ac_cnlList");
		//获取all
		ArrayList all = (ArrayList) scatterPlotClu.get("all");
		
		show2DAttr(ac_cnlList,all,-1, "Order of data points", "Product of local density and distance");
	}

	/**
	 * 根据散点数据绘制散点图
	 * 
	 * @param ac_cnlList
	 * 					添加所有待展示数据,所有数据点都放在一个list中，由all指定数据点所属的类别
	 * @param all
	 * 			添加类别和所含数据条数
     * @param lineValue
     *          水平虚线的纵坐标，-1为不画线
	 */
	public static void show2DAttr(LinkedList ac_cnlList,ArrayList all, int lineValue, String xAxisLabel, String yAxisLabel) {
        
        DefaultXYDataset xydataset = new DefaultXYDataset();   
          
        //创建主题样式    
        StandardChartTheme mChartTheme = new StandardChartTheme("unicode");
        //设置标题字体    
//        mChartTheme.setExtraLargeFont(new Font("Times New Roman", Font.PLAIN, 25));
        //设置轴向字体    
        mChartTheme.setLargeFont(new Font("Times New Roman", Font.BOLD, 17));
        //设置图例字体    
        mChartTheme.setRegularFont(new Font("Times New Roman", Font.BOLD, 15));
        //应用主题样式
        ChartFactory.setChartTheme(mChartTheme);    
          
        //根据实际需求加载数据集到xydatasets中  ,l为类别标签  
        for(int l = 0; l < all.size(); l++){  
          
            int size = ((Set) all.get(l)).size();  
            double [][]datas = new double[2][size];  
            int m =0;  
            for(Iterator it = ((Set) all.get(l)).iterator();it.hasNext();m++){  
                HashMap line = ((HashMap)ac_cnlList.get((Integer) it.next()));  
                double AC = (Double) line.get("AC");
                double CNL =  (Double) line.get("CNL");  
                datas[0][m] = AC;    //x轴  
                datas[1][m] = CNL;   //y轴  
              
            }  
            xydataset.addSeries(l, datas);  //l为类别标签  
              
        }  
  
        JFreeChart chart = ChartFactory.createScatterPlot(null, xAxisLabel, yAxisLabel, xydataset, PlotOrientation.VERTICAL, false, false, false);
        ChartFrame frame = new ChartFrame("Scatter Diagram ", chart, true);//图标题
        chart.setBackgroundPaint(Color.white);
        chart.setBorderPaint(Color.GREEN);      
        chart.setBorderStroke(new BasicStroke(1.5f));      
        XYPlot xyplot = (XYPlot) chart.getPlot();

        //画水平虚线
        if(lineValue >=0) {
            ValueMarker marker = new ValueMarker(lineValue, Color.green, new BasicStroke(
                    1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{6.0f, 6.0f}, 0.0f
            ));

            marker.setLabel("    T_"+ yAxisLabel +" = "+ lineValue+"    ");//标签内容
            marker.setLabelFont(new Font("SansSerif", 3, 15));
            marker.setLabelOffsetType(LengthAdjustmentType.NO_CHANGE);
            marker.setLabelPaint(Color.BLACK);//字体颜色
            marker.setLabelAnchor(RectangleAnchor.CENTER);//文本框位置
            marker.setLabelTextAnchor(TextAnchor.BOTTOM_CENTER);//字体在文本框中的位置
            marker.setLabelOffset(new RectangleInsets(10, 0, 5, 0));//设置时间标签显示的位置
            xyplot.addRangeMarker(marker);
        }

        //设置曲线是否显示数据点
        XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyplot.getRenderer();
        xylineandshaperenderer.setDefaultShapesVisible(true);
        xylineandshaperenderer.setSeriesOutlinePaint(0, Color.WHITE);
        xylineandshaperenderer.setUseOutlinePaint(true);
        NumberAxis numberaxis = (NumberAxis) xyplot.getDomainAxis();
        numberaxis.setAutoRangeIncludesZero(false);
        numberaxis.setTickMarkInsideLength(2.0F);
        numberaxis.setTickMarkOutsideLength(0.0F);
//        numberaxis.setAutoTickUnitSelection(false);
//        numberaxis.setTickUnit(new NumberTickUnit(1D)); //数据轴的数据标签（需要将AutoTickUnitSelection设false）

        //设置曲线显示各数据点的值
        XYItemRenderer xyitem = xyplot.getRenderer();
        xyitem.setDefaultItemLabelsVisible(true);
        xyitem.setDefaultItemLabelGenerator(new StandardXYItemLabelGenerator());
        xyitem.setDefaultItemLabelFont(new Font("Dialog", 1, 12));

        xyplot.setRenderer(xyitem);

        //点的形状
//        renderer.setSeriesShape(1, new Rectangle2D.Double(-3.0,-3.0, 6.0, 6.0));//方块
//        renderer.setSeriesShape(0, new Line2D.Double(0.0, 0.0,0.0, 0.0));//点
        //设置图片背景色
        xyplot.setBackgroundPaint(new Color(255, 253, 246));

        ValueAxis va = xyplot.getDomainAxis(0);//获取x轴
        va.setAxisLineStroke(new BasicStroke(1.5f)); // 坐标轴粗细       <span style="white-space:pre"> </span>
        va.setAxisLinePaint(new Color(215, 215, 215));    // 坐标轴颜色      
        xyplot.setOutlineStroke(new BasicStroke(1.5f));   // 边框粗细      
        va.setLabelPaint(new Color(10, 10, 10));          // 坐标轴标题颜色      
        va.setTickLabelPaint(new Color(102, 102, 102));   // 坐标轴标尺值颜色      
        ValueAxis axis = xyplot.getRangeAxis();//获取Y轴
        axis.setAxisLineStroke(new BasicStroke(1.5f));      
      
        //设置chart尺寸
        frame.setSize(new Dimension(1124, 810));
        frame.setResizable(true);
        frame.getChartPanel().setPopupMenu(null);
        frame.getChartPanel().setRangeZoomable(false);
        frame.setVisible(true);

//        writeChartToPDF(chart, PageSize.A4.getHeight(),PageSize.A4.getWidth(), "F:\\Users\\chuan\\Desktop\\"+yAxisLabel+".pdf");
    }

    /**
     * 使用itext将JFreechart输出到PDF
     * @param chart
     * @param width
     * @param height
     * @param fileName
     */
    public static void writeChartToPDF(JFreeChart chart, float width, float height, String fileName) {
        PdfWriter writer = null;

//        Document document = new Document();
        Document document = new Document(PageSize.A4.rotate());//横向打印

        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(
                    fileName));
            document.open();
            PdfContentByte contentByte = writer.getDirectContent();
            PdfTemplate template = contentByte.createTemplate(width, height);
            Graphics2D graphics2d = template.createGraphics(width, height,
                    new DefaultFontMapper());
            Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width,
                    height);

            chart.draw(graphics2d, rectangle2d);

            graphics2d.dispose();
            contentByte.addTemplate(template, 0, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        document.close();
    }
}
