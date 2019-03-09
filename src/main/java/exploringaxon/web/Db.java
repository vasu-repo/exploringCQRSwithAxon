package exploringaxon.web;

import exploringaxon.model.Account;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;

/**
 * Adds the two account types needed.
 * Created by Dadepo Aderemi.
 */
@Component
public class Db {

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @Autowired
    private Repository repository;

    @Autowired
    private javax.sql.DataSource dataSource;

    @PostConstruct
    private void init(){
        // init the event store

        // delete previous events on startup
        FileSystemUtils.deleteRecursively(Paths.get("./events").toFile());

        TransactionTemplate transactionTmp = new TransactionTemplate(txManager);
        transactionTmp.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                UnitOfWork uow = DefaultUnitOfWork.startAndGet(null);
                uow.execute(() -> {
                    try {
                        repository.newInstance(() -> new Account("acc-one"));
                        repository.newInstance(() -> new Account("acc-two"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            }
        });

        // init the tables for query/view
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create table account_view (account_no VARCHAR , balance FLOAT )");
        jdbcTemplate.update("insert into account_view (account_no, balance) values (?, ?)", new Object[]{"acc-one", 0.0});
        jdbcTemplate.update("insert into account_view (account_no, balance) values (?, ?)", new Object[]{"acc-two", 0.0});
    }

}
