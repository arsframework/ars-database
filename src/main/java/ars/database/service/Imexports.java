package ars.database.service;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.lang.reflect.Field;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import ars.util.Beans;
import ars.util.Dates;
import ars.util.Files;
import ars.util.Nfile;
import ars.util.Strings;
import ars.util.AbstractTimerServer;
import ars.invoke.Invokes;
import ars.invoke.request.Requester;
import ars.file.office.Excels;
import ars.database.repository.Query;

/**
 * 数据导入/导出操作工具类
 *
 * @author wuyongqiang
 */
public final class Imexports {
    /**
     * 数据导入/导出文件存放目录
     */
    private static final File DIRECTORY = new File(Strings.TEMP_PATH, "imexport");

    /**
     * 文件名称正则表达式匹配模式
     */
    private static final Pattern PATTERN = Pattern
        .compile("[0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12}.xlsx");

    static {
        if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
        }
        AbstractTimerServer cleaner = new AbstractTimerServer() {

            @Override
            protected void execute() throws Exception {
                DIRECTORY.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        if (file.isFile() && PATTERN.matcher(file.getName()).matches()
                            && System.currentTimeMillis() - file.lastModified() >= 24 * 60 * 60 * 1000) {
                            file.delete();
                        }
                        return false;
                    }

                });
            }

        };
        cleaner.setInterval(60 * 60);
        cleaner.start();
    }

    private Imexports() {

    }

    /**
     * 数据导入/导出结果类
     *
     * @author wuyongqiang
     */
    public static class Result {
        private int total; // 总数目
        private int failed; // 失败数目
        private String file; // 文件名称
        private String size; // 文件大小
        private String spend; // 操作耗时

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getSpend() {
            return spend;
        }

        public void setSpend(String spend) {
            this.spend = spend;
        }

    }

    /**
     * 获取Excel数据对象适配器
     *
     * @param <T>   数据类型
     * @param model 数据模型
     * @return Excel数据对象适配器
     */
    public static <T> ExcelAdapter<T> getExcelAdapter(final Class<T> model) {
        if (model == null) {
            throw new IllegalArgumentException("Model must not be null");
        }
        final Field[] fields = Beans.getFields(model);
        return new ExcelAdapter<T>() {

            @Override
            public String[] getTitles(Requester requester, Service<T> service) {
                return Invokes.getPropertyMessages(requester, model);
            }

            @Override
            public T read(Requester requester, Service<T> service, Row row, int count) {
                return Excels.getObject(row, model, fields);
            }

            @Override
            public void write(Requester requester, Service<T> service, T entity, Row row, int count) {
                Excels.setObject(row, entity, fields);
            }

        };
    }

    /**
     * 数据批量导入
     *
     * @param <T>       数据类型
     * @param requester 请求对象
     * @param service   业务处理对象
     * @param file      文件对象
     * @param start     开始数据行（从0开始）
     * @return 数据导入结果对象
     * @throws Exception 操作异常
     */
    public static <T> Result input(Requester requester, Service<T> service, Nfile file, int start) throws Exception {
        if (service == null) {
            throw new IllegalArgumentException("Service must not be null");
        }
        return input(requester, service, file, start, getExcelAdapter(service.getModel()));
    }

    /**
     * 数据批量导入
     *
     * @param <T>       数据类型
     * @param requester 请求对象
     * @param service   业务处理对象
     * @param file      文件对象
     * @param start     开始数据行（从0开始）
     * @param adapter   Excel文件适配对象
     * @return 数据导入结果对象
     * @throws Exception 操作异常
     */
    public static <T> Result input(final Requester requester, final Service<T> service, final Nfile file,
                                   final int start, final ExcelAdapter<T> adapter) throws Exception {
        if (requester == null) {
            throw new IllegalArgumentException("Requester must not be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("Service must not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("Nfile must not be null");
        }
        if (start < 0) {
            throw new IllegalArgumentException("Start must not be less than 0");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("ExcelAdapter must not be null");
        }
        long timestamp = System.currentTimeMillis();
        final Result result = new Result();
        Workbook workbook = Excels.getWorkbook(file);
        final Workbook failed = new SXSSFWorkbook(100);
        try {
            final String[] titles = start > 0 ? Excels.getTitles(workbook, start - 1) : Strings.EMPTY_ARRAY;
            int count = Excels.iteration(workbook, start, new Excels.Reader<T>() {

                @Override
                public T read(Row row, int count) {
                    try {
                        T entity = adapter.read(requester, service, row, count);
                        if (entity != null) {
                            service.saveObject(requester, entity);
                        }
                    } catch (Exception e) {
                        Row target = null;
                        if (result.getFailed() % 50000 == 0) {
                            Sheet sheet = failed.createSheet();
                            if (titles.length > 0) {
                                Excels.setTitles(sheet.createRow(0), titles);
                            }
                            target = sheet.createRow(start);
                        } else {
                            Sheet sheet = failed.getSheetAt(failed.getNumberOfSheets() - 1);
                            target = sheet.createRow(sheet.getLastRowNum() + 1);
                        }
                        Excels.copy(row, target);
                        int columns = titles.length > 0 ? titles.length : row.getLastCellNum();
                        Excels.setValue(target.createCell(columns), e.getMessage());
                        result.setFailed(result.getFailed() + 1);
                    }
                    return null;
                }
            });
            result.setTotal(count);
            if (result.getFailed() > 0) {
                String name = new StringBuilder(UUID.randomUUID().toString()).append(".xlsx").toString();
                File attachment = new File(DIRECTORY, name);
                Excels.write(failed, attachment);
                result.setFile(name);
                result.setSize(Files.getUnitSize(attachment.length()));
            }
        } finally {
            try {
                workbook.close();
            } finally {
                failed.close();
            }
        }
        result.setSpend(Dates.getUnitTime(System.currentTimeMillis() - timestamp));
        return result;
    }

    /**
     * 数据批量导出
     *
     * @param <T>       数据类型
     * @param requester 请求对象
     * @param service   业务处理对象
     * @return 导出结果
     * @throws Exception 操作异常
     */
    public static <T> Result output(Requester requester, Service<T> service) throws Exception {
        if (service == null) {
            throw new IllegalArgumentException("Service must not be null");
        }
        return output(requester, service, getExcelAdapter(service.getModel()));
    }

    /**
     * 数据批量导出
     *
     * @param <T>       数据类型
     * @param requester 请求对象
     * @param service   业务处理对象
     * @param adapter   Excel文件适配对象
     * @return 导出结果
     * @throws Exception 操作异常
     */
    public static <T> Result output(Requester requester, final Service<T> service, ExcelAdapter<T> adapter)
        throws Exception {
        if (requester == null) {
            throw new IllegalArgumentException("Requester must not be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("Service must not be null");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("ExcelAdapter must not be null");
        }
        long timestamp = System.currentTimeMillis();
        String[] titles = adapter.getTitles(requester, service);
        final Result result = new Result();
        Workbook workbook = new SXSSFWorkbook(100);
        try {
            if (requester.hasParameter(Query.PAGE) && requester.hasParameter(Query.SIZE)) {
                Sheet sheet = workbook.createSheet();
                Excels.setTitles(sheet.createRow(0), titles);
                List<T> objects = service.getQuery(requester).custom(requester.getParameters()).list();
                for (int i = 0; i < objects.size(); i++) {
                    adapter.write(requester, service, objects.get(i), sheet.createRow(i + 1), i + 1);
                }
                result.setTotal(objects.size());
            } else {
                Sheet sheet = null;
                int r = 1, count = 0, length = 200;
                int total = service.getQuery(requester).custom(requester.getParameters()).count();
                for (int i = 1, pages = (int) Math.ceil((double) total / length); i <= pages; i++) {
                    List<T> objects = service.getQuery(requester).custom(requester.getParameters()).paging(i, length)
                        .list();
                    for (int n = 0; n < objects.size(); n++) {
                        if (++count % 50000 == 1) {
                            r = 1;
                            sheet = workbook.createSheet();
                            Excels.setTitles(sheet.createRow(0), titles);
                        }
                        adapter.write(requester, service, objects.get(n), sheet.createRow(r++), count);
                    }
                }
                result.setTotal(count);
            }
            String name = new StringBuilder(UUID.randomUUID().toString()).append(".xlsx").toString();
            File attachment = new File(DIRECTORY, name);
            Excels.write(workbook, attachment);
            result.setFile(name);
            result.setSize(Files.getUnitSize(attachment.length()));
            result.setSpend(Dates.getUnitTime(System.currentTimeMillis() - timestamp));
        } finally {
            workbook.close();
        }
        return result;
    }

    /**
     * 下载导入/导出文件
     *
     * @param name 文件名称
     * @return 文件对象
     */
    public static File download(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        File file = new File(DIRECTORY, name);
        return file.exists() ? file : null;
    }

}
