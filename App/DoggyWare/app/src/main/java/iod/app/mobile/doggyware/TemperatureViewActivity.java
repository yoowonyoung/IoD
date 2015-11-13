package iod.app.mobile.doggyware;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;

import com.handstudio.android.hzgrapherlib.animation.GraphAnimation;
import com.handstudio.android.hzgrapherlib.graphview.CurveGraphView;
import com.handstudio.android.hzgrapherlib.vo.GraphNameBox;
import com.handstudio.android.hzgrapherlib.vo.curvegraph.CurveGraph;
import com.handstudio.android.hzgrapherlib.vo.curvegraph.CurveGraphVO;

import java.util.ArrayList;
import java.util.List;

public class TemperatureViewActivity extends AppCompatActivity {
    private ViewGroup layoutGraphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temparature_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layoutGraphView = (ViewGroup) findViewById(R.id.layoutGraphView);
        setCurveGraph();
        System.out.println("SEX");
    }

    private void setCurveGraph() {
        CurveGraphVO vo = makeCurveGraphAllSetting();
        final CurveGraphView cgv = new CurveGraphView(this, vo);
        /* Graph View Size 강제 변경
        ViewTreeObserver vto = cgv.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                cgv.getViewTreeObserver().removeOnPreDrawListener(this);
                int width = cgv.getMeasuredWidth();
                Log.i("ImageViewSize", "Width : " + Integer.toString(width));
                cgv.getLayoutParams().height = (width / 16) * 10;
                return true;
            }
        });
        */
        layoutGraphView.addView(cgv);
    }

    private CurveGraphVO makeCurveGraphAllSetting() {
        //padding
        int paddingBottom 	= CurveGraphVO.DEFAULT_PADDING;
        int paddingTop 		= CurveGraphVO.DEFAULT_PADDING;
        int paddingLeft 	= CurveGraphVO.DEFAULT_PADDING;
        int paddingRight 	= CurveGraphVO.DEFAULT_PADDING;

        //graph margin
        int marginTop 		= CurveGraphVO.DEFAULT_MARGIN_TOP;
        int marginRight 	= CurveGraphVO.DEFAULT_MARGIN_RIGHT;

        //max value
        int maxValue 		= 25;

        //increment
        int increment 		= 5;

        //GRAPH SETTING
        String[] legendArr 	= {"15시","16시","17시","18시","19시"};
        float[] graph1 		= {21,22,20,19,19};
        float[] graph2 		= {5,7,7,6,7};
        float[] graph3 		= {14,15,14,13,16};

        List<CurveGraph> arrGraph 		= new ArrayList<CurveGraph>();

        arrGraph.add(new CurveGraph("거실", 0xaa66ff33, graph1));
        arrGraph.add(new CurveGraph("안방", 0xaa00ffff, graph2));
        arrGraph.add(new CurveGraph("내방", 0xaaff0066, graph3));

        CurveGraphVO vo = new CurveGraphVO(
                paddingBottom, paddingTop, paddingLeft, paddingRight,
                marginTop, marginRight, maxValue, increment, legendArr, arrGraph);
        vo.setAnimation(new GraphAnimation(GraphAnimation.LINEAR_ANIMATION, GraphAnimation.DEFAULT_DURATION));
        vo.setGraphNameBox(new GraphNameBox());
        return vo;
    }
}