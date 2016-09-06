# ViewPagerDemo
[前往博客查看更多](http://www.jianshu.com/p/2a8a298caf5f)
> 最近在项目中用到ViewPager+FragmentPagerAdapter的方式来做界面，其中当adapter的数据源数据更新时，调用adapter.notifyDataSetChanged()更新数据，发现ViewPager并没有更新，还是原来的数据。

参考了别人的文章以及部分解决的的方法，加上自己的理解，拿出了下面这套解决方案。

目录：
1. 问题展示
2. 解决方案
3. 问题追究

### 问题展示
国际惯例，先上问题图（图一）以及正常图（图二）。
![图一](http://upload-images.jianshu.io/upload_images/1787089-e970888491c9cb39.gif?imageMogr2/auto-orient/strip)
![图二](http://upload-images.jianshu.io/upload_images/1787089-78c04653935a0ab6.gif?imageMogr2/auto-orient/strip)
### 解决方案
1. 在数据源更新的前面加入以下代码
```
if (viewPager.getAdapter() != null) {
    FragmentManager fm = getSupportFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    List<Fragment> fragments = fm.getFragments();
    if(fragments != null && fragments.size() >0){
        for (int i = 0; i < fragments.size(); i++) {
            ft.remove(fragments.get(i));
        }
    }
    ft.commit();}
```
2. 在你的adapter类中加入以下代码
```
private int mChildCount = 0;
@Overridepublic void notifyDataSetChanged() {
    // 重写这个方法，取到子Fragment的数量，用于下面的判断，以执行多少次刷新    
    mChildCount = getCount();
    super.notifyDataSetChanged();
}
@Override
public int getItemPosition(Object object) {
    if ( mChildCount > 0) {
        // 这里利用判断执行若干次不缓存，刷新
        mChildCount --;
        // 返回这个是强制ViewPager不缓存，每次滑动都刷新视图
        return POSITION_NONE;
    }
    // 这个则是缓存不刷新视图
    return super.getItemPosition(object);}
```
##### 较完整代码一览
初始化数据
```
viewPager= (ViewPager) findViewById(R.id.pager);
mList=new ArrayList<Fragment>();
for (int i=1;i<4;i++){
    Bundle bundle=new Bundle();
    bundle.putString("text","第"+i+"页");
    MyFragment myFragment=new MyFragment();
    myFragment.setArguments(bundle);
    mList.add(myFragment);
}
adapter=new MyAdapter(getSupportFragmentManager());
viewPager.setAdapter(adapter);
```
自定义adapter
```
public class MyAdapter extends FragmentPagerAdapter{
    public MyAdapter(FragmentManager fm) {
        super(fm);
    }
    @Override    public Fragment getItem(int position) {
        return mList.get(position);
    }
    @Override    public int getCount() {
        return mList.size();
    }
    // start
    // 可以删除这段代码看看，数据源更新而viewpager不更新的情况
    private int mChildCount = 0;
    @Override    public void notifyDataSetChanged() {
        // 重写这个方法，取到子Fragment的数量，用于下面的判断，以执行多少次刷新
        mChildCount = getCount();
        super.notifyDataSetChanged();
    }
    @Override    public int getItemPosition(Object object) {
        if ( mChildCount > 0) {
            // 这里利用判断执行若干次不缓存，刷新
            mChildCount --;
            // 返回这个是强制ViewPager不缓存，每次滑动都刷新视图
            return POSITION_NONE;
        }
        // 这个则是缓存不刷新视图
        return super.getItemPosition(object);
    }
    // end
}
```
更新数据源的方法
```
public void update(){
    // start
    // 可以删除这段代码看看，数据源更新而viewpager不更新的情况
    // 在数据源更新前增加的代码，将上一次数据源的fragment对象从FragmentManager中删除
    if (viewPager.getAdapter() != null) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        List<Fragment> fragments = fm.getFragments();
        if(fragments != null && fragments.size() >0){
            for (int i = 0; i < fragments.size(); i++) {
                ft.remove(fragments.get(i));
            }
        }
        ft.commit();
    }
    // End
    mList.clear();
    for (int i=4;i<7;i++){
        Bundle bundle=new Bundle();
        bundle.putString("text","第"+i+"页");
        MyFragment myFragment=new MyFragment();
        myFragment.setArguments(bundle);
        mList.add(myFragment);
    }
    // 重写adapter的notifyDataChanged方法
    adapter.notifyDataSetChanged();}
```
##### demo源码下载
[前往github下载源码](https://github.com/SamanLan/ViewPagerDemo)
可根据注释删除对应的代码，体验有问题以及正常的情况。

### 问题追究
首先来理解两个adapter，都是继承与pageradapter

1. **FragmentPagerAdapter**：该类更专注于每一页均为 Fragment 的情况。**该类内的每一个生成的 Fragment 都将保存在内存之中**，因此适用于那些相对静态的页，数量也比较少的那种；如果需要处理有很多页，并且数据动态性较大、占用内存较多的情况，应该使用``FragmentStatePagerAdapter``。

2. **FragmentStatePagerAdapter**：和 ``FragmentPagerAdapter ``不一样的是，正如其类名中的 'State' 所表明的含义一样，该 PagerAdapter 的实现将只保留当前页面，当页面离开视线后，就会被消除，释放其资源；而在页面需要显示时，生成新的页面(就像 ListView 的实现一样)。这么实现的好处就是当拥有大量的页面时，不必在内存中占用大量的内存。

3. 这两个adapter最大的不同在于``instantiateItem()``这个方法


接下来看看adapter里面getItemPosition这个方法
可以返回的值为``POSITION_UNCHANGED``和``POSITION_NONE``这两个值。
而默认都是返回``POSITION_UNCHANGED``
这个返回值会在adapter的``instantiateItem()``方法里进行判断：
``POSITION_UNCHANGED``不重新加载item
``POSITION_NONE``要求重新加载item

而网上的一些解决方案是直接复写``FragmentPagerAdapter``的``getItemPosition``返回``POSITION_NONE``，这样做及违反了``FragmentPagerAdapter``的设计原则（保存在内存，加载更快等）也没有解决今天这个坑，一样是界面没有刷新的。

继续说下去

假如返回``POSITION_NONE``要求从新加载Item，ViewPager会首先去``FragmentManager``里面去查找有没有相关的``fragment``如果有就直接使用如果没有才会触发``FragmentPageadApter``的``getItem``方法获取一个``fragment``。所以你更新的fragmentList集合是没有作用的，还要清除``FragmentManager``里面缓存的``fragment``。

这样今天的解决方案思路救出来了：
1. 复写``notifyDataSetChanged``
```
@Override
public void notifyDataSetChanged() {
    // 重写这个方法，取到子Fragment的数量，用于下面的判断，以执行多少次刷新
    mChildCount = getCount();
    super.notifyDataSetChanged();
}
```
2. 复写``getItemPosition``，根据``mChildCount ``判断是返回``POSITION_UNCHANGED``还是``itemPOSITION_NONE``
```
@Override
public int getItemPosition(Object object) {
    if ( mChildCount > 0) {
        // 这里利用判断执行若干次不缓存，刷新
        mChildCount --;
        // 返回这个是itemPOSITION_NONE
        return POSITION_NONE;
    }
    // 这个则是POSITION_UNCHANGED
    return super.getItemPosition(object);}
```
3. 在``notifyDataSetChanged``之前对``FragmentManager``进行相应的删除操作。
```
if (viewPager.getAdapter() != null) {
    FragmentManager fm = getSupportFragmentManager();
    FragmentTransaction ft = fm.beginTransaction();
    List<Fragment> fragments = fm.getFragments();
    if(fragments != null && fragments.size() >0){
        for (int i = 0; i < fragments.size(); i++) {
            ft.remove(fragments.get(i));
        }
    }
    ft.commit();}
```
4. 这样就会在``notifyDataSetChanged``的时候刷新视图，在平时滑动等情况使用缓存视图，既保留了``FragmentPagerAdapter``的特点，又解决了今天的坑。

***
到此，今天的坑又总算是跨过去了，如果有帮组到你，欢迎关注[我的博客](http://www.jianshu.com/users/25018a1e0b12/)和[github](https://github.com/SamanLan)
[源码下载](https://github.com/SamanLan/ViewPagerDemo)
本文参考自：
* http://www.cnblogs.com/lianghui66/p/3607091.html
* http://blog.sina.com.cn/s/blog_783ede03010173b4.html

***
2016年9月7日 00:35:49
