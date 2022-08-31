package iped.viewers.timelinegraph.popups;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.entity.XYItemEntity;

import iped.app.ui.Messages;
import iped.data.IItemId;
import iped.engine.data.IPEDSource;
import iped.viewers.api.IMultiSearchResultProvider;
import iped.viewers.timelinegraph.IpedChartPanel;
import iped.viewers.timelinegraph.datasets.IpedTimelineDataset;
import iped.viewers.timelinegraph.swingworkers.BitSetSelectWorker;
import iped.viewers.timelinegraph.swingworkers.EventPeriodCheckWorker;
import iped.viewers.timelinegraph.swingworkers.SelectWorker;


public class DataItemPopupMenu extends JPopupMenu implements ActionListener {
	XYItemEntity chartEntity;
	IpedChartPanel ipedChartPanel;

	JMenuItem selectEventItens;
	JMenuItem selectPeriodItens;
	JMenuItem checkEventItens;
	JMenuItem checkPeriodItens;

	List<XYItemEntity> entityList;

	public DataItemPopupMenu(IpedChartPanel ipedChartPanel) {
		this.ipedChartPanel = ipedChartPanel;
		
		selectEventItens = new JMenuItem(Messages.getString("TimeLineGraph.selectEventItensOnPeriod"));
		selectEventItens.addActionListener(this);
        add(selectEventItens);

		selectPeriodItens = new JMenuItem(Messages.getString("TimeLineGraph.selectItensOnPeriod"));
        selectPeriodItens.addActionListener(this);
        add(selectPeriodItens); 

        checkEventItens = new JMenuItem(Messages.getString("TimeLineGraph.checkEventItensOnPeriod"));
        checkEventItens.addActionListener(this);
        add(checkEventItens);

        checkPeriodItens = new JMenuItem(Messages.getString("TimeLineGraph.checkItensOnPeriod"));
        checkPeriodItens.addActionListener(this);
        add(checkPeriodItens); 
	}	

	public XYItemEntity getChartEntity() {
		return chartEntity;
	}

	public void setChartEntity(XYItemEntity chartEntity) {
		this.chartEntity = chartEntity;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==selectEventItens) {
			Calendar c = Calendar.getInstance(ipedChartPanel.getIpedChartsPanel().getTimeZone());
			c.setTime(new Date((long)chartEntity.getDataset().getXValue(chartEntity.getSeriesIndex(), chartEntity.getItem())));
			Calendar cEnd = (Calendar) c.clone();
			cEnd.add(Calendar.DAY_OF_MONTH, 1);
			
			IMultiSearchResultProvider msrp = ipedChartPanel.getIpedChartsPanel().getResultsProvider();
			IPEDSource is =  (IPEDSource) msrp.getIPEDSource();
			
			BitSet bs = new BitSet();			
			IpedTimelineDataset ds = (IpedTimelineDataset) chartEntity.getDataset();
			List<IItemId> ids = ds.getItems(chartEntity.getItem(), chartEntity.getSeriesIndex());        			
			if(ids!=null) {
				for (Iterator iterator = ids.iterator(); iterator.hasNext();) {
					IItemId iItemId = (IItemId) iterator.next();
					bs.set(is.getLuceneId(iItemId));
				}
			}

			BitSetSelectWorker bsSelect = new BitSetSelectWorker(ipedChartPanel.getIpedChartsPanel().getDomainAxis(), msrp, bs, true);
			bsSelect.execute();
		}
		
		if(e.getSource()==selectPeriodItens) {
			Calendar c = Calendar.getInstance(ipedChartPanel.getIpedChartsPanel().getTimeZone());
			c.setTime(new Date((long)chartEntity.getDataset().getXValue(chartEntity.getSeriesIndex(), chartEntity.getItem())));
			Calendar cEnd = (Calendar) c.clone();
			cEnd.add(Calendar.DAY_OF_MONTH, 1);
			ipedChartPanel.getIpedChartsPanel().selectItemsOnInterval(c.getTime(), cEnd.getTime(), true);
		}
		if(e.getSource()==checkEventItens) {
			Calendar c = Calendar.getInstance(ipedChartPanel.getIpedChartsPanel().getTimeZone());
			c.setTime(new Date((long)chartEntity.getDataset().getXValue(chartEntity.getSeriesIndex(), chartEntity.getItem())));
			Calendar cEnd = (Calendar) c.clone();
			cEnd.add(Calendar.DAY_OF_MONTH, 1);
			
			IMultiSearchResultProvider msrp = ipedChartPanel.getIpedChartsPanel().getResultsProvider();
			IPEDSource is =  (IPEDSource) msrp.getIPEDSource();
			
			BitSet bs = new BitSet();			
			IpedTimelineDataset ds = (IpedTimelineDataset) chartEntity.getDataset();
			List<IItemId> ids = ds.getItems(chartEntity.getItem(), chartEntity.getSeriesIndex());        			
			if(ids!=null) {
				for (Iterator iterator = ids.iterator(); iterator.hasNext();) {
					IItemId iItemId = (IItemId) iterator.next();
					bs.set(is.getLuceneId(iItemId));
				}
			}

			EventPeriodCheckWorker bsSelect = new EventPeriodCheckWorker(ipedChartPanel.getIpedChartsPanel().getDomainAxis(), msrp, bs, true);
			bsSelect.execute();
		}
		if(e.getSource()==checkPeriodItens) {
			long timestamp = (long)chartEntity.getDataset().getXValue(chartEntity.getSeriesIndex(), chartEntity.getItem());
			Date start = new Date(timestamp);
			Date end = new Date(timestamp+(long)ipedChartPanel.getIpedChartsPanel().getTimePeriodLength()-1l);
			ipedChartPanel.getIpedChartsPanel().checkItemsOnInterval(start, end, false);
		}
	}

	public void setChartEntityList(List<XYItemEntity> entityList) {
		this.entityList=entityList;
	}

}
