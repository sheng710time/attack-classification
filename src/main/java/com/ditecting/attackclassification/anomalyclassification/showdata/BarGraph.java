package com.ditecting.attackclassification.anomalyclassification.showdata;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;


public class BarGraph {

    public static void main(String[] args) {
// 使用普通数据集
        DefaultCategoryDataset chartDate = new DefaultCategoryDataset();
        // 增加测试数据，第一个参数是访问量，最后一个是时间，中间是显示用不考虑
        chartDate.addValue(0.997726, "Accuracy", "Accuracy");
        chartDate.addValue(0.996258, "Precision", "Precision");
        chartDate.addValue(0.995744, "Recall", "Recall");
        chartDate.addValue(0.995989, "F1", "F1");
        try {
            // 从数据库中获得数据集
            DefaultCategoryDataset data = chartDate;

            // 使用ChartFactory创建3D柱状图，不想使用3D，直接使用createBarChart
            JFreeChart chart = ChartFactory.createBarChart(
                    "", // 图表标题
                    "Evaluation Index", // 目录轴的显示标签
                    "Score", // 数值轴的显示标签
                    data, // 数据集
                    PlotOrientation.VERTICAL, // 图表方向，此处为垂直方向
                    // PlotOrientation.HORIZONTAL, //图表方向，此处为水平方向
                    true, // 是否显示图例
                    true, // 是否生成工具
                    false // 是否生成URL链接
            );
            ChartFrame frame = new ChartFrame("Scatter Diagram ", chart, true);//图标题

            // 设置整个图片的背景色
            chart.setBackgroundPaint(Color.PINK);
            // 设置图片有边框
            chart.setBorderVisible(false);
            Font kfont = new Font("宋体", Font.PLAIN, 12);    // 底部
            Font titleFont = new Font("宋体", Font.BOLD, 25); // 图片标题
            // 图片标题
            chart.setTitle(new TextTitle(chart.getTitle().getText(), titleFont));
            // 底部
            chart.getLegend().setItemFont(kfont);
            // 得到坐标设置字体解决乱码
            CategoryPlot categoryplot = (CategoryPlot) chart.getPlot();
            categoryplot.setDomainGridlinesVisible(true);
            categoryplot.setRangeCrosshairVisible(true);
            categoryplot.setRangeCrosshairPaint(Color.blue);
            NumberAxis numberaxis = (NumberAxis) categoryplot.getRangeAxis();
            numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            BarRenderer barrenderer = (BarRenderer) categoryplot.getRenderer();
//            barrenderer.setSeriesItemLabelFont(1, new Font("宋体", Font.PLAIN, 18));
            barrenderer.setMaximumBarWidth(0.5);
            barrenderer.setItemMargin(0);
            CategoryAxis domainAxis = categoryplot.getDomainAxis();
            /*------设置X轴坐标上的文字-----------*/
            domainAxis.setTickLabelFont(new Font("sans-serif", Font.PLAIN, 11));
            /*------设置X轴的标题文字------------*/
            domainAxis.setLabelFont(new Font("宋体", 3, 12));
            /*------设置Y轴坐标上的文字-----------*/
            numberaxis.setTickLabelFont(new Font("sans-serif", Font.PLAIN, 12));
            /*------设置Y轴的标题文字------------*/
            numberaxis.setLabelFont(new Font("宋体", 3, 12));
            /*------这句代码解决了底部汉字乱码的问题-----------*/
            chart.getLegend().setItemFont(new Font("宋体", Font.PLAIN, 12));

            //设置frame尺寸
//            frame.setSize(new Dimension(500, 350));
            frame.setSize(new Dimension(1124, 810));
            frame.setResizable(true);
            frame.getChartPanel().setPopupMenu(null);
            frame.getChartPanel().setRangeZoomable(false);
            frame.setVisible(true);

            writeChartToPDF(chart, PageSize.A4.getHeight(),PageSize.A4.getWidth(), "F:\\Users\\chuan\\Desktop\\fig10.pdf");
//            ChartPanel panel=new ChartPanel(chart);
//            panel.setSize(550,400);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
