# Android自定义控件（一）——自定义下拉刷新控件

标签（空格分隔）： Android

---

原文：https://www.zybuluo.com/Tyhj/note/800026

自定义控件大家都会用，网上也一大堆，有些的确很炫酷，但是很难遇到在自己的项目中想要的效果。所以会一些基本的自定义控件还是必要的。我也是刚开始学习自定义控件，有兴趣的可以看看，交流，指出我的不足之处

下拉刷新控件是一个经常被自定义的控件，可以实现很炫的效果，一般情况，Google官方的SwipeRefreshLayout就很好用了，现在我要实现的效果如下：

<figure class="fourth">
<div>
<img src="http://ac-fgtnb2h8.clouddn.com/d476abb84d5ca496fd40.gif" width="200" height="100"/>
    </div>
</figure>

gif可能有点慢，在实际效果很流畅的，显示效果也可能更好。
刚开始做自定义控件一开始就想自己从零实现真的难，所以先看看别人是怎么做的，然后自己照着模仿，改动，然后自己实现。我先看了[郭霖的自定义ListView下拉刷新控件](http://blog.csdn.net/guolin_blog/article/details/9255575)自己改的，然后了解了其中过程，基本上这类的下拉刷新效果自己去实现就没什么问题了。

### 实现思路：
这里我们将采取的方案是使用组合View的方式，先自定义一个布局继承自LinearLayout，然后在这个布局中加入下拉头和ListView这两个子元素，并让这两个子元素纵向排列。初始化的时候，让下拉头向上偏移出屏幕，这样我们看到的就只有ListView了。然后对ListView的touch事件进行监听，如果当前ListView已经滚动到顶部并且手指还在向下拉的话，那就将下拉头显示出来，松手后进行刷新操作，并将下拉头隐藏。原理示意图如下：

![](http://img.blog.csdn.net/20130706113543125?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvc2lueXU4OTA4MDc=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

先新建一个布局作为下拉的头部：
```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/pull_to_refresh_head"
    android:layout_width="match_parent"
    android:layout_height="360dp">

    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="360dp"
        android:elevation="2dp"
        android:gravity="center">

        <ImageView
            android:id="@+id/iv_triangle"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginTop="130dp"
            android:src="@mipmap/ic_triangle" />

    </LinearLayout>


    <ImageView
        android:layout_width="match_parent"
        android:layout_height="360dp"
        android:scaleType="centerCrop"
        android:src="@mipmap/bg_refresh" />


</RelativeLayout>

```


里面很简单就是一个ImageView显示刷新的那个旋转的三角形，另一个就是下拉头的背景。

然后新建一个RefreshableView继承自LinearLayout，代码如下所示：
```java
package myview;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yorhp.refreshview.R;

import static android.R.attr.fromDegrees;
import static android.R.attr.pivotX;
import static android.R.attr.pivotY;
import static android.R.attr.toDegrees;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class RefreshableViewList extends LinearLayout implements View.OnTouchListener {

    /**
     * 下拉状态
     */
    public static final int STATUS_PULL_TO_REFRESH = 0;

    /**
     * 释放立即刷新状态
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 1;

    /**
     * 正在刷新状态
     */
    public static final int STATUS_REFRESHING = 2;

    /**
     * 刷新完成或未刷新状态
     */
    public static final int STATUS_REFRESH_FINISHED = 3;

    /**
     * 下拉头部回滚的速度
     */
    public static final int SCROLL_SPEED = -30;


    /**
     * 下拉的长度
     */

    private int pullLength;


    /**
     * 下拉刷新的回调接口
     */
    private PullToRefreshListener mListener;


    /**
     * 下拉头的View
     */
    private View header;

    /**
     * 需要去下拉刷新的ListView
     */
    private ListView listView;

    /**
     * 刷新时显示的进度条
     */
   // private ProgressBar progressBar;


    //三角形
    private ImageView iv_triangle;

    /**
     * 指示下拉和释放的箭头
     */
   // private ImageView arrow;

    /**
     * 指示下拉和释放的文字描述
     */
    //private TextView description;


    /**
     * 下拉头的布局参数
     */
    private MarginLayoutParams headerLayoutParams;


    /**
     * 为了防止不同界面的下拉刷新在上次更新时间上互相有冲突，使用id来做区分
     */
    private int mId = -1;

    /**
     * 下拉头的高度
     */
    private int hideHeaderHeight;

    /**
     * 当前处理什么状态，可选值有STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
     * STATUS_REFRESHING 和 STATUS_REFRESH_FINISHED
     */
    private int currentStatus = STATUS_REFRESH_FINISHED;
    ;

    /**
     * 记录上一次的状态是什么，避免进行重复操作
     */
    private int lastStatus = currentStatus;

    /**
     * 手指按下时的屏幕纵坐标
     */
    private float yDown;

    /**
     * 在被判定为滚动之前用户手指可以移动的最大值。
     */
    private int touchSlop;

    /**
     * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
     */
    private boolean loadOnce;

    /**
     * 当前是否可以下拉，只有ListView滚动到头的时候才允许下拉
     */
    private boolean ableToPull;

    /**
     * 下拉刷新控件的构造函数，会在运行时动态添加一个下拉头的布局。
     *
     * @param context
     * @param attrs
     */
    public RefreshableViewList(Context context, AttributeSet attrs) {
        super(context, attrs);
        header = LayoutInflater.from(context).inflate(R.layout.pull_to_refresh, null, true);
        iv_triangle = (ImageView) header.findViewById(R.id.iv_triangle);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOrientation(VERTICAL);
        addView(header, 0);
    }

    /**
     * 进行一些关键性的初始化操作，比如：将下拉头向上偏移进行隐藏，给ListView注册touch事件。
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && !loadOnce) {
            hideHeaderHeight = -header.getHeight();
            pullLength = hideHeaderHeight / 4 * 3;
            headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
            headerLayoutParams.topMargin = hideHeaderHeight;
            listView = (ListView) getChildAt(1);
            listView.setOnTouchListener(this);
            loadOnce = true;
        }
    }


    int preDistance = 0;

    /**
     * 当ListView被触摸时调用，其中处理了各种下拉刷新的具体逻辑。
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        setIsAbleToPull(event);
        if (ableToPull) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDown = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float yMove = event.getRawY();
                    int distance = (int) (yMove - yDown);
                    // 如果手指是下滑状态，并且下拉头是完全隐藏的，就屏蔽下拉事件
                    if (distance <= pullLength && headerLayoutParams.topMargin <= hideHeaderHeight) {
                        return false;
                    }
                    if (distance < touchSlop) {
                        return false;
                    }
                    if (currentStatus != STATUS_REFRESHING) {
                        if (headerLayoutParams.topMargin > pullLength) {
                            currentStatus = STATUS_RELEASE_TO_REFRESH;
                        } else {
                            currentStatus = STATUS_PULL_TO_REFRESH;
                        }
                        // 通过偏移下拉头的topMargin值，来实现下拉效果，修改分母可以有不同的拉力效果
                        headerLayoutParams.topMargin = (int) ((distance / 2.8) + hideHeaderHeight);
                        header.setLayoutParams(headerLayoutParams);
                        //添加动画（这里是手指控制滑动的动画）、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、
                        rotateTriangle((distance - preDistance)/2);
                        preDistance=distance;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                default:
                    if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
                        // 松手时如果是释放立即刷新状态，就去调用正在刷新的任务
                        new RefreshingTask().execute();
                    } else if (currentStatus == STATUS_PULL_TO_REFRESH) {
                        // 松手时如果是下拉状态，就去调用隐藏下拉头的任务
                        new HideHeaderTask().execute();
                    }
                    break;
            }
            // 时刻记得更新下拉头中的信息  
            if (currentStatus == STATUS_PULL_TO_REFRESH
                    || currentStatus == STATUS_RELEASE_TO_REFRESH) {
                updateHeaderView();
                // 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态  
                listView.setPressed(false);
                listView.setFocusable(false);
                listView.setFocusableInTouchMode(false);
                lastStatus = currentStatus;
                // 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件  
                return true;
            }
        }
        return false;
    }

    /**
     * 给下拉刷新控件注册一个监听器。
     *
     * @param listener 监听器的实现。
     * @param id       为了防止不同界面的下拉刷新在上次更新时间上互相有冲突， 请不同界面在注册下拉刷新监听器时一定要传入不同的id。
     */
    public void setOnRefreshListener(PullToRefreshListener listener, int id) {
        mListener = listener;
        mId = id;
    }

    /**
     * 当所有的刷新逻辑完成后，记录调用一下，否则你的ListView将一直处于正在刷新状态。
     */
    public void finishRefreshing() {
        currentStatus = STATUS_REFRESH_FINISHED;
        new HideHeaderTask().execute();
    }

    /**
     * 根据当前ListView的滚动状态来设定 {@link #ableToPull}
     * 的值，每次都需要在onTouch中第一个执行，这样可以判断出当前应该是滚动ListView，还是应该进行下拉。
     *
     * @param event
     */
    private void setIsAbleToPull(MotionEvent event) {
        View firstChild = listView.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = listView.getFirstVisiblePosition();
            if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
                if (!ableToPull) {
                    yDown = event.getRawY();
                }
                // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新  
                ableToPull = true;
            } else {
                if (headerLayoutParams.topMargin != hideHeaderHeight) {
                    headerLayoutParams.topMargin = hideHeaderHeight;
                    header.setLayoutParams(headerLayoutParams);
                }
                ableToPull = false;
            }
        } else {
            // 如果ListView中没有元素，也应该允许下拉刷新  
            ableToPull = true;
        }
    }

    /**
     * 更新下拉头中的信息。
     */
    private void updateHeaderView() {
        if (lastStatus != currentStatus) {
            if (currentStatus == STATUS_PULL_TO_REFRESH) {  //下拉状态

            } else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {  //释放状态

            } else if (currentStatus == STATUS_REFRESHING) {  //刷新中

                iv_triangle.clearAnimation();
                TriangelRotate();
            }
        }
    }

    float preDegres = 0;

    //手指下拉的时候的动画
    private void rotateTriangle(float angle) {
        float pivotX = iv_triangle.getWidth() /2;
        float pivotY = (float) (iv_triangle.getHeight() /1.6);
        float fromDegrees = preDegres;
        float toDegrees = angle;

        RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees, pivotX, pivotY);
        animation.setDuration(10);
        animation.setFillAfter(true);
        iv_triangle.startAnimation(animation);
        preDegres = preDegres + angle;
    }

    //刷新的时候一直转的动画
    private void TriangelRotate(){
        float pivotX = iv_triangle.getWidth() /2;
        float pivotY = (float) (iv_triangle.getHeight() /1.6);
        RotateAnimation animation = new RotateAnimation(0f, 120f, pivotX, pivotY);
        animation.setDuration(50);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        preDegres = 0;
        LinearInterpolator linearInterpolator=new LinearInterpolator();
        animation.setInterpolator(linearInterpolator);
        iv_triangle.startAnimation(animation);
    }




    /**
     * 正在刷新的任务，在此任务中会去回调注册进来的下拉刷新监听器。
     * 下拉超过了，要返回到刷新的位置
     *
     * @author guolin
     */
    class RefreshingTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (true) {
                topMargin = topMargin + SCROLL_SPEED;
                if (topMargin <= pullLength) {
                    topMargin = pullLength;
                    break;
                }
                publishProgress(topMargin);
                sleep(10);
            }
            currentStatus = STATUS_REFRESHING;
            publishProgress(pullLength);
            if (mListener != null) {
                mListener.onRefresh();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... topMargin) {
            updateHeaderView();
            headerLayoutParams.topMargin = topMargin[0];
            header.setLayoutParams(headerLayoutParams);
            //添加动画（这里是手指松开后返回到刷新位置的动画）、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、
        }

    }

    /**
     * 隐藏下拉头的任务，当未进行下拉刷新或下拉刷新完成后，此任务将会使下拉头重新隐藏。
     *
     * @author guolin
     */
    class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (true) {
                topMargin = topMargin + SCROLL_SPEED;
                if (topMargin <= hideHeaderHeight) {
                    topMargin = hideHeaderHeight;
                    break;
                }
                publishProgress(topMargin);
                sleep(10);
            }
            return topMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... topMargin) {
            headerLayoutParams.topMargin = topMargin[0];
            header.setLayoutParams(headerLayoutParams);
            //添加动画（这里是手指松开后返回到初始位置的动画）、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、

        }

        @Override
        protected void onPostExecute(Integer topMargin) {
            headerLayoutParams.topMargin = topMargin;
            header.setLayoutParams(headerLayoutParams);
            currentStatus = STATUS_REFRESH_FINISHED;
            //完成刷新、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、、
            iv_triangle.clearAnimation();
        }
    }

    /**
     * 使当前线程睡眠指定的毫秒数。
     *
     * @param time 指定当前线程睡眠多久，以毫秒为单位
     */
    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下拉刷新的监听器，使用下拉刷新的地方应该注册此监听器来获取刷新回调。
     *
     * @author guolin
     */
    public interface PullToRefreshListener {

        /**
         * 刷新时会去回调此方法，在方法内编写具体的刷新逻辑。注意此方法是在子线程中调用的， 你可以不必另开线程来进行耗时操作。
         */
        void onRefresh();

    }

}  
```

### 代码说明
这个类是整个下拉刷新功能中最重要的一个类，注释已经写得比较详细了，我再简单解释一下。首先在RefreshableView的构造函数中动态添加了刚刚定义的pull_to_refresh这个布局作为下拉头，然后在onLayout方法中将下拉头向上偏移出了屏幕，再给ListView注册了touch事件。之后每当手指在ListView上滑动时，onTouch方法就会执行。在onTouch方法中的第一行就调用了setIsAbleToPull方法来判断ListView是否滚动到了最顶部，只有滚动到了最顶部才会执行后面的代码，否则就视为正常的ListView滚动，不做任何处理。当ListView滚动到了最顶部时，如果手指还在向下拖动，就会改变下拉头的偏移值，让下拉头显示出来，下拉的距离设定为手指移动距离的1/2.8，这样才会有拉力的感觉。如果下拉的距离足够大，在松手的时候就会执行刷新操作，如果距离不够大，就仅仅重新隐藏下拉头。


具体的刷新操作会在RefreshingTask中进行，其中在doInBackground方法中回调了PullToRefreshListener接口的onRefresh方法，这也是大家在使用RefreshableView时必须要去实现的一个接口，因为具体刷新的逻辑就应该写在onRefresh方法中，后面会演示使用的方法。



你可能一下子看起来觉得很多代码，但是如果你把自己实现的那些可以修改的代码删除后看起来就简单很多了。然后你再把自己的逻辑添加进去，你的代码可能别人看起来也很难的。最重要的是自己要亲手做，简单地看可能怎么也看不会。

下拉刷新那自然要适配RecyclerView和NestedScrollView，实现起来也很简单，就是在判定列表是否滑动到顶部的时候代码改动一下。

```java
//ListView判定方法
  private void setIsAbleToPull(MotionEvent event) {
        View firstChild = listView.getChildAt(0);
        if (firstChild != null) {
            int firstVisiblePos = listView.getFirstVisiblePosition();
            if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
                if (!ableToPull) {
                    yDown = event.getRawY();
                }
                // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新  
                ableToPull = true;
            } else {
                if (headerLayoutParams.topMargin != hideHeaderHeight) {
                    headerLayoutParams.topMargin = hideHeaderHeight;
                    header.setLayoutParams(headerLayoutParams);
                }
                ableToPull = false;
            }
        } else {
            // 如果ListView中没有元素，也应该允许下拉刷新  
            ableToPull = true;
        }
    }
    
//RecyclerView判定方法
 private void setIsAbleToPull(MotionEvent event) {
        View firstChild = listView.getChildAt(0);
        if (firstChild != null) {
            LinearLayoutManager lm = (LinearLayoutManager) listView.getLayoutManager();
            int firstVisiblePos = lm.findFirstVisibleItemPosition();
            if (firstVisiblePos == 0 && firstChild.getTop() == 0) {
                if (!ableToPull) {
                    yDown = event.getRawY();
                }
                // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新  
                ableToPull = true;
            } else {
                if (headerLayoutParams.topMargin != hideHeaderHeight) {
                    headerLayoutParams.topMargin = hideHeaderHeight;
                    header.setLayoutParams(headerLayoutParams);
                }
                ableToPull = false;
            }
        } else {
            // 如果ListView中没有元素，也应该允许下拉刷新  
            ableToPull = true;
        }
    }
    
    
//NestedScrollView

private void setIsAbleToPull(MotionEvent event) {
        View firstChild = listView.getChildAt(0);
        if (firstChild != null) {
            int scrollY = listView.getScrollY();
            if (scrollY == 0 && firstChild.getTop() == 0) {
                if (!ableToPull) {
                    yDown = event.getRawY();
                }
                // 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新  
                ableToPull = true;
            } else {
                if (headerLayoutParams.topMargin != hideHeaderHeight) {
                    headerLayoutParams.topMargin = hideHeaderHeight;
                    header.setLayoutParams(headerLayoutParams);
                }
                ableToPull = false;
            }
        } else {
            // 如果ListView中没有元素，也应该允许下拉刷新  
            ableToPull = true;
        }
    }

```


添加动画和改变状态什么的重要的地方我注释出来了，自己去搞搞还是很有意思的。


参考文章：[Android下拉刷新完全解析，教你如何一分钟实现下拉刷新功能](http://blog.csdn.net/guolin_blog/article/details/9255575)

项目地址：[Tyhj的可自定义下拉刷新控件](https://github.com/tyhjh/RefreshView)











