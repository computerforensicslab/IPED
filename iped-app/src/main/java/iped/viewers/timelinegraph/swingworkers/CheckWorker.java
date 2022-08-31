package iped.viewers.timelinegraph.swingworkers;

import java.util.Date;

import javax.swing.JTable;

import iped.app.ui.App;
import iped.app.ui.BookmarksController;
import iped.viewers.api.IMultiSearchResultProvider;
import iped.viewers.timelinegraph.IpedDateAxis;

/*
 *  Extends SelectWorker, so the bitset of docids is mounted based on date interval. This bitset is used internally to check the docids.
 */
public class CheckWorker extends SelectWorker {

	public CheckWorker(IpedDateAxis domainAxis, IMultiSearchResultProvider resultsProvider, Date start, Date end,
			boolean select, boolean clearPreviousSelection) {
		super(domainAxis, resultsProvider, start, end, select, clearPreviousSelection);
	}

	public CheckWorker(IpedDateAxis domainAxis, IMultiSearchResultProvider resultsProvider, Date start, Date end,
			boolean clearPreviousSelection) {
		this(domainAxis, resultsProvider, start, end, true, clearPreviousSelection);
	}

	@Override
	public void processResultsItem(JTable t, int i) {
        Boolean checked = (Boolean) t.getModel().getValueAt(i, 1);
        t.getModel().setValueAt(!checked.booleanValue(), i, 1);
	}

	@Override
	protected void doSelect() {
		BookmarksController.get().setMultiSetting(true);
		super.doSelect();
	}

	@Override
	protected void done() {
		BookmarksController.get().setMultiSetting(false);
		resultsProvider.getIPEDSource().getMultiBookmarks().saveState();
        BookmarksController.get().updateUISelection();
		super.done();
	}
	
	public void clear() {
		//t.clearSelection();
	}

}
