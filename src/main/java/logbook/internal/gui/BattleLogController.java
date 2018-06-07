package logbook.internal.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import logbook.bean.BattleLog;
import logbook.bean.BattleTypes.CombinedType;
import logbook.bean.BattleTypes.IFormation;
import logbook.bean.BattleTypes.IMidnightBattle;
import logbook.bean.MapStartNext;
import logbook.bean.Ship;
import logbook.bean.SlotItem;
import logbook.internal.BattleLogs;
import logbook.internal.BattleLogs.IUnit;
import logbook.internal.BattleLogs.SimpleBattleLog;
import logbook.internal.BattleLogs.Unit;
import logbook.internal.LoggerHolder;
import logbook.internal.ToStringConverter;

/**
 * 戦闘ログのUIコントローラー
 *
 */
public class BattleLogController extends WindowController {

    /** 統計 */
    @FXML
    private TreeTableView<BattleLogCollect> collect;

    /** 集計 */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> unit;

    /** 出撃  */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> start;

    /** 勝利  */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> win;

    /** S勝利 */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> s;

    /** A勝利 */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> a;

    /** B勝利 */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> b;

    /** C敗北 */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> c;

    /** D敗北 */
    @FXML
    private TreeTableColumn<BattleLogCollect, String> d;

    /** フィルタ */
    @FXML
    private FlowPane filterPane;

    /** 詳細 */
    @FXML
    private TableView<BattleLogDetail> detail;

    /** 日付 */
    @FXML
    private TableColumn<BattleLogDetail, String> date;

    /** 海域 */
    @FXML
    private TableColumn<BattleLogDetail, String> area;

    /** マス */
    @FXML
    private TableColumn<BattleLogDetail, String> cell;

    /** ボス */
    @FXML
    private TableColumn<BattleLogDetail, String> boss;

    /** 評価 */
    @FXML
    private TableColumn<BattleLogDetail, String> rank;

    /** 艦隊行動 */
    @FXML
    private TableColumn<BattleLogDetail, String> intercept;

    /** 味方陣形 */
    @FXML
    private TableColumn<BattleLogDetail, String> fformation;

    /** 敵陣形 */
    @FXML
    private TableColumn<BattleLogDetail, String> eformation;

    /** 制空権 */
    @FXML
    private TableColumn<BattleLogDetail, String> dispseiku;

    /** 味方触接 */
    @FXML
    private TableColumn<BattleLogDetail, String> ftouch;

    /** 敵触接 */
    @FXML
    private TableColumn<BattleLogDetail, String> etouch;

    /** 敵艦隊 */
    @FXML
    private TableColumn<BattleLogDetail, String> efleet;

    /** ドロップ艦種 */
    @FXML
    private TableColumn<BattleLogDetail, String> dropType;

    /** ドロップ艦娘 */
    @FXML
    private TableColumn<BattleLogDetail, String> dropShip;

    /** ドロップアイテム */
    @FXML
    private TableColumn<BattleLogDetail, String> dropItem;

    /** 種類 */
    @FXML
    private ChoiceBox<String> aggregateType;

    /** 集計 */
    @FXML
    private TableView<BattleLogDetailAggregate> aggregate;

    /** 種類 */
    @FXML
    private TableColumn<BattleLogDetailAggregate, String> type;

    /** 合計 */
    @FXML
    private TableColumn<BattleLogDetailAggregate, Long> count;

    /** 割合 */
    @FXML
    private TableColumn<BattleLogDetailAggregate, Double> ratio;

    /** 集計 */
    @FXML
    private PieChart chart;

    /** 戦闘ログ */
    private Map<IUnit, List<SimpleBattleLog>> logMap;

    /** 詳細(フィルタ前) */
    private ObservableList<BattleLogDetail> detailsSource = FXCollections.observableArrayList();

    /** 詳細(フィルタ済み) */
    private FilteredList<BattleLogDetail> filteredDetails = new FilteredList<>(this.detailsSource);

    /** 集計用Map */
    private Map<String, Function<BattleLogDetail, ?>> aggregateTypeMap = new HashMap<>();

    @FXML
    void initialize() {
        try {
            TableTool.setVisible(this.detail, this.getClass().toString() + "#" + "detail");
            TableTool.setVisible(this.aggregate, this.getClass().toString() + "#" + "aggregate");

            // 統計
            this.collect.setShowRoot(false);

            this.unit.setCellValueFactory(new TreeItemPropertyValueFactory<>("unit"));
            this.start.setCellValueFactory(new TreeItemPropertyValueFactory<>("start"));
            this.win.setCellValueFactory(new TreeItemPropertyValueFactory<>("win"));
            this.s.setCellValueFactory(new TreeItemPropertyValueFactory<>("s"));
            this.a.setCellValueFactory(new TreeItemPropertyValueFactory<>("a"));
            this.b.setCellValueFactory(new TreeItemPropertyValueFactory<>("b"));
            this.c.setCellValueFactory(new TreeItemPropertyValueFactory<>("c"));
            this.d.setCellValueFactory(new TreeItemPropertyValueFactory<>("d"));

            // 選択された時のリスナーを設定
            this.collect.getSelectionModel()
                    .selectedItemProperty()
                    .addListener(this::detail);

            // 詳細
            this.detail.setRowFactory(tv -> {
                TableRow<BattleLogDetail> r = new TableRow<>();
                r.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && (!r.isEmpty())) {
                        BattleLogDetail d = r.getItem();
                        BattleLog log = BattleLogs.read(d.getDate());
                        if (log != null) {
                            try {
                                MapStartNext last = log.getNext().get(log.getNext().size() - 1);
                                CombinedType combinedType = log.getCombinedType();
                                Map<Integer, List<Ship>> deckMap = log.getDeckMap();
                                Map<Integer, SlotItem> itemMap = log.getItemMap();
                                IFormation battle = log.getBattle();
                                IMidnightBattle midnight = log.getMidnight();
                                Set<Integer> escape = log.getEscape();

                                InternalFXMLLoader.showWindow("logbook/gui/battle_detail.fxml", this.getWindow(),
                                        "戦闘ログ", c -> {
                                            ((BattleDetail) c).setData(last, combinedType, deckMap, escape, itemMap,
                                                    battle, midnight);
                                        }, null);
                            } catch (Exception ex) {
                                LoggerHolder.get().error("詳細の表示に失敗しました", ex);
                            }
                        }
                    }
                });
                return r;
            });
            this.detail.setItems(this.filteredDetails);
            this.detail.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            this.detail.setOnKeyPressed(TableTool::defaultOnKeyPressedHandler);

            this.date.setCellValueFactory(new PropertyValueFactory<>("date"));
            this.area.setCellValueFactory(new PropertyValueFactory<>("area"));
            this.cell.setCellValueFactory(new PropertyValueFactory<>("cell"));
            this.boss.setCellValueFactory(new PropertyValueFactory<>("boss"));
            this.rank.setCellValueFactory(new PropertyValueFactory<>("rank"));
            this.intercept.setCellValueFactory(new PropertyValueFactory<>("intercept"));
            this.fformation.setCellValueFactory(new PropertyValueFactory<>("fformation"));
            this.eformation.setCellValueFactory(new PropertyValueFactory<>("eformation"));
            this.dispseiku.setCellValueFactory(new PropertyValueFactory<>("dispseiku"));
            this.ftouch.setCellValueFactory(new PropertyValueFactory<>("ftouch"));
            this.etouch.setCellValueFactory(new PropertyValueFactory<>("etouch"));
            this.efleet.setCellValueFactory(new PropertyValueFactory<>("efleet"));
            this.dropType.setCellValueFactory(new PropertyValueFactory<>("dropType"));
            this.dropShip.setCellValueFactory(new PropertyValueFactory<>("dropShip"));
            this.dropItem.setCellValueFactory(new PropertyValueFactory<>("dropItem"));

            // 統計
            // ルート要素(非表示)
            TreeItem<BattleLogCollect> root = new TreeItem<BattleLogCollect>(new BattleLogCollect());
            this.collect.setRoot(root);

            // 集計
            this.aggregate.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            this.aggregate.setOnKeyPressed(TableTool::defaultOnKeyPressedHandler);
            // 種類
            this.type.setCellValueFactory(new PropertyValueFactory<>("name"));
            // 合計
            this.count.setCellValueFactory(new PropertyValueFactory<>("count"));
            // 割合
            this.ratio.setCellValueFactory(new PropertyValueFactory<>("ratio"));
            // 集計初期化
            this.initializeAggregate();

            // ログの読み込み
            this.setCollect();

            // フィルタ
            this.initializeFilterPane();
        } catch (Exception e) {
            LoggerHolder.get().error("FXMLの初期化に失敗しました", e);
        }
    }

    /**
     * ログをセット
     */
    private void setCollect() {
        // 集計単位がキーのマップ
        this.logMap = BattleLogs.readSimpleLog();
        for (IUnit unit : Unit.values()) {
            addTree(unit);
        }
    }

    /**
     * ログをセット
     * @param unit 集計単位
     */
    private void addTree(IUnit unit) {
        List<SimpleBattleLog> list = this.logMap.get(unit);

        // 単位のルート
        BattleLogCollect unitRootValue = BattleLogs.collect(list, null, false);
        unitRootValue.setUnit(unit.getName());
        unitRootValue.setCollectUnit(unit);

        TreeItem<BattleLogCollect> unitRoot = new TreeItem<BattleLogCollect>(unitRootValue);
        unitRoot.setExpanded(true);

        // ボス
        BattleLogCollect bossValue = BattleLogs.collect(list, null, true);
        bossValue.setUnit("ボス");
        bossValue.setCollectUnit(unit);
        bossValue.setBoss(true);

        TreeItem<BattleLogCollect> boss = new TreeItem<BattleLogCollect>(bossValue);
        unitRoot.getChildren().add(boss);

        // 海域の名前
        List<String> areaNames = list.stream()
                .map(SimpleBattleLog::getArea)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        for (String area : areaNames) {
            // 海域毎の集計
            BattleLogCollect areaValue = BattleLogs.collect(list, area, false);
            areaValue.setUnit(area);
            areaValue.setCollectUnit(unit);
            areaValue.setArea(area);

            TreeItem<BattleLogCollect> areaRoot = new TreeItem<BattleLogCollect>(areaValue);

            // 海域ボス
            BattleLogCollect areaBossValue = BattleLogs.collect(list, area, true);
            areaBossValue.setUnit("ボス");
            areaBossValue.setCollectUnit(unit);
            areaBossValue.setArea(area);
            areaBossValue.setBoss(true);

            TreeItem<BattleLogCollect> areaBoss = new TreeItem<BattleLogCollect>(areaBossValue);
            areaRoot.getChildren().add(areaBoss);

            unitRoot.getChildren().add(areaRoot);
        }

        this.collect.getRoot().getChildren().add(unitRoot);
    }

    /**
     * ログの更新
     */
    @FXML
    void reloadAction(ActionEvent event) {
        int selectedIndex = this.collect.getSelectionModel().getSelectedIndex();
        // 中身をクリア
        this.collect.getRoot().getChildren().clear();

        this.setCollect();
        this.collect.getSelectionModel().focus(selectedIndex);
        this.collect.getSelectionModel().select(selectedIndex);
    }

    /**
     * クリップボードにコピー
     */
    @FXML
    void copyDetail() {
        TableTool.selectionCopy(this.detail);
    }

    /**
     * すべてを選択
     */
    @FXML
    void selectAllDetail() {
        TableTool.selectAll(this.detail);
    }

    /**
     * テーブル列の表示・非表示の設定
     */
    @FXML
    void columnVisibleDetail() {
        try {
            TableTool.showVisibleSetting(this.detail, this.getClass().toString() + "#" + "detail", this.getWindow());
        } catch (Exception e) {
            LoggerHolder.get().error("FXMLの初期化に失敗しました", e);
        }
    }

    /**
     * クリップボードにコピー
     */
    @FXML
    void copyAggregate() {
        TableTool.selectionCopy(this.aggregate);
    }

    /**
     * すべてを選択
     */
    @FXML
    void selectAllAggregate() {
        TableTool.selectAll(this.aggregate);
    }

    /**
     * テーブル列の表示・非表示の設定
     */
    @FXML
    void columnVisibleAggregate() {
        try {
            TableTool.showVisibleSetting(this.aggregate, this.getClass().toString() + "#" + "aggregate",
                    this.getWindow());
        } catch (Exception e) {
            LoggerHolder.get().error("FXMLの初期化に失敗しました", e);
        }
    }

    /**
     * 右ペインに詳細表示するリスナー
     *
     * @param observable 値が変更されたObservableValue
     * @param oldValue 古い値
     * @param value 新しい値
     */
    private void detail(ObservableValue<? extends TreeItem<BattleLogCollect>> observable,
            TreeItem<BattleLogCollect> oldValue, TreeItem<BattleLogCollect> value) {
        this.detailsSource.clear();
        if (value != null) {
            BattleLogCollect collect = value.getValue();
            String area = collect.getArea();
            boolean boss = collect.isBoss();

            Predicate<BattleLogDetail> anyFilter = e -> true;
            // 海域フィルタ
            Predicate<BattleLogDetail> areaFilter = area != null ? e -> area.equals(e.getArea()) : anyFilter;
            // ボスフィルタ
            Predicate<BattleLogDetail> bossFilter = boss ? e -> e.getBoss().indexOf("ボス") != -1 : anyFilter;

            this.detailsSource.addAll(this.logMap.get(collect.getCollectUnit())
                    .stream()
                    .map(BattleLogDetail::toBattleLogDetail)
                    .filter(areaFilter)
                    .filter(bossFilter)
                    .sorted(Comparator.comparing(BattleLogDetail::getDate).reversed())
                    .collect(Collectors.toList()));
        }
        this.initializeFilterPane();
    }

    /**
     * フィルターを初期化する
     */
    private void initializeFilterPane() {
        this.filterPane.getChildren().clear();
        this.filteredDetails.setPredicate(null);
        List<Predicate<BattleLogDetail>> filterBase = new ArrayList<>();
        ListChangeListener<Object> listener = c -> {
            Predicate<BattleLogDetail> predicate = null;
            for (Predicate<BattleLogDetail> filter : filterBase) {
                if (predicate == null) {
                    predicate = filter;
                } else {
                    predicate = predicate.and(filter);
                }
            }
            this.filteredDetails.setPredicate(predicate);
        };
        filterBase.add(this.addFilterColumn(this.detail, this.area, listener, BattleLogDetail::getArea));
        filterBase.add(this.addFilterColumn(this.detail, this.cell, listener, BattleLogDetail::getCell));
        filterBase.add(this.addFilterColumn(this.detail, this.boss, listener, BattleLogDetail::getBoss));
        filterBase.add(this.addFilterColumn(this.detail, this.rank, listener, BattleLogDetail::getRank));
        filterBase.add(this.addFilterColumn(this.detail, this.intercept, listener, BattleLogDetail::getIntercept));
        filterBase.add(this.addFilterColumn(this.detail, this.fformation, listener, BattleLogDetail::getFformation));
        filterBase.add(this.addFilterColumn(this.detail, this.eformation, listener, BattleLogDetail::getEformation));
        filterBase.add(this.addFilterColumn(this.detail, this.dispseiku, listener, BattleLogDetail::getDispseiku));
        filterBase.add(this.addFilterColumn(this.detail, this.ftouch, listener, BattleLogDetail::getFtouch));
        filterBase.add(this.addFilterColumn(this.detail, this.etouch, listener, BattleLogDetail::getEtouch));
        filterBase.add(this.addFilterColumn(this.detail, this.efleet, listener, BattleLogDetail::getEfleet));
        filterBase.add(this.addFilterColumn(this.detail, this.dropType, listener, BattleLogDetail::getDropType));
        filterBase.add(this.addFilterColumn(this.detail, this.dropShip, listener, BattleLogDetail::getDropShip));
        filterBase.add(this.addFilterColumn(this.detail, this.dropItem, listener, BattleLogDetail::getDropItem));
    }

    /**
     * 列をフィルターに追加する
     */
    private <S, T> Predicate<S> addFilterColumn(TableView<S> table, TableColumn<S, T> column,
            ListChangeListener<Object> listener, Function<S, T> getter) {
        VBox box = new VBox();
        box.getChildren().add(new Label(Tools.Tables.getColumnName(column)));
        CheckComboBox<T> comboBox = new CheckComboBox<>();
        comboBox.setConverter(new ToStringConverter<>(v -> {
            String str = v.toString();
            if (str.isEmpty())
                return "(空白)";
            return str;
        }));
        comboBox.getItems().addAll(
                table.getItems().stream()
                        .map(column::getCellData)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList()));
        box.getChildren().add(comboBox);
        this.filterPane.getChildren().add(box);

        comboBox.getCheckModel()
                .getCheckedItems()
                .addListener(listener);
        Predicate<S> filter = o -> {
            return comboBox.getCheckModel().getCheckedItems().isEmpty()
                    || comboBox.getCheckModel().getCheckedItems().contains(getter.apply(o));
        };
        return filter;
    }

    /**
     * 集計を初期化する
     */
    private void initializeAggregate() {
        this.addAggregate(this.area, BattleLogDetail::getArea);
        this.addAggregate(this.cell, BattleLogDetail::getCell);
        this.addAggregate(this.boss, BattleLogDetail::getBoss);
        this.addAggregate(this.rank, BattleLogDetail::getRank);
        this.addAggregate(this.intercept, BattleLogDetail::getIntercept);
        this.addAggregate(this.fformation, BattleLogDetail::getFformation);
        this.addAggregate(this.eformation, BattleLogDetail::getEformation);
        this.addAggregate(this.dispseiku, BattleLogDetail::getDispseiku);
        this.addAggregate(this.ftouch, BattleLogDetail::getFtouch);
        this.addAggregate(this.etouch, BattleLogDetail::getEtouch);
        this.addAggregate(this.efleet, BattleLogDetail::getEfleet);
        this.addAggregate(this.dropType, BattleLogDetail::getDropType);
        this.addAggregate(this.dropShip, BattleLogDetail::getDropShip);
        this.addAggregate(this.dropItem, BattleLogDetail::getDropItem);

        // 列の選択が変更されたときに集計する
        this.aggregateType.getSelectionModel()
                .selectedItemProperty()
                .addListener(c -> this.aggregate());
        // 右ペインの詳細が変化したときに集計する
        this.filteredDetails.addListener((ListChangeListener<Object>) c -> this.aggregate());
    }

    /**
     * 列を集計対象に追加する
     */
    private void addAggregate(TableColumn<BattleLogDetail, ?> column, Function<BattleLogDetail, ?> getter) {
        String name = Tools.Tables.getColumnName(column);
        this.aggregateType.getItems().add(name);
        this.aggregateTypeMap.put(name, getter);
    }

    /**
     * 集計する
     */
    private void aggregate() {
        String name = this.aggregateType.getSelectionModel()
                .getSelectedItem();
        ObservableList<BattleLogDetailAggregate> items = FXCollections.observableArrayList();
        ObservableList<PieChart.Data> value = FXCollections.observableArrayList();
        if (name != null) {
            Function<BattleLogDetail, ?> getter = this.aggregateTypeMap.get(name);
            Map<String, Long> result = this.filteredDetails.stream()
                    .map(getter)
                    .map(String::valueOf)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            double total = result.values().stream()
                    .mapToLong(Long::longValue)
                    .sum();
            for (Entry<String, Long> entry : result.entrySet()) {
                String type = entry.getKey();
                if (type.isEmpty()) {
                    type = "(空白)";
                }
                // テーブル行
                BattleLogDetailAggregate row = new BattleLogDetailAggregate();
                row.setName(type);
                row.setCount(entry.getValue());
                row.setRatio(entry.getValue() / total * 100);
                items.add(row);
                // チャート
                value.add(new PieChart.Data(type, entry.getValue()));
            }
            items.sort(Comparator.comparing(BattleLogDetailAggregate::getName));
            value.sort(Comparator.comparing(PieChart.Data::getPieValue).reversed());
        }
        this.aggregate.setItems(items);
        this.chart.setData(value);
    }
}
