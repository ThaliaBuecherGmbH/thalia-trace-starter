/*
 * (c) Copyright 2018 Thalia Bücher GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.thalia.boot.tracing.database;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

import de.thalia.boot.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

/**
 * {@link BeanPostProcessor} der {@link DataSource}-Instanzen für das Tracing mit einer {@link ProxyDataSource} wrappt, mit deren
 * Hilfe die Aktionen auf der Datenquelle nachvollzogen werden können.
 *
 * @author Hendrik Busch
 * @since 21.02.18
 */
@Slf4j
@RequiredArgsConstructor
public class DatasourceWrappingBeanPostProcessor implements BeanPostProcessor, Ordered {

    /**
     * Suffix für den Namen der Datenquelle, wie er im Tracing auftaucht.
     */
    private static final String SPAN_SUFFIX = "-DataSource";

    /**
     * Der Tracer, mit dem die Daten aufgezeichnet werden.
     */
    private final Tracer tracer;

    /**
     * Vor der Initialisierung der Datasource brauchen wir nichts zu tun, daher reicht diese Methode die Bean nur durch.
     * 
     * @param bean
     *            die erzeugte Bean
     * @param beanName
     *            der Name der erzeugten Bean
     * @return {@code bean}
     */
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }

    /**
     * Wrappt die übergebene Bean in eine {@link net.ttddyy.dsproxy.support.ProxyDataSource}, sofern es sich um eine Datasource
     * handelt und konfiguriert den Tracing-Listener für diese Datasource.
     * 
     * @param bean
     *            die evtl. zu wrappende Bean
     * @param beanName
     *            der Name der Bean
     * @return eine {@link net.ttddyy.dsproxy.support.ProxyDataSource} falls es sich bei der Bean um eine Datasource handelte,
     *         ansonsten die Bean einfach unverändert.
     */
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        if (!DataSource.class.isAssignableFrom(bean.getClass())) {
            return bean;
        } else if (ProxyDataSource.class.isAssignableFrom(bean.getClass())) {
            log.warn(
                    "postProcessAfterInitialization() - DataSource {} ist bereits eine ProxyDataSource, verzichte auf ProxyDataSource-ception...",
                    beanName);
            return bean;
        }

        log.debug("postProcessBeforeInitialization() - Wrappe DataSource '{}'", beanName);

        final DataSource dataSource = (DataSource) bean;
        return ProxyDataSourceBuilder.create(dataSource).name(beanName)
                .listener(new TracingQueryExecutionListener(tracer)).build();

    }

    /**
     * Sicherstellen, dass dieser Processor vor irgendwelchen DataSources initialisiert wird, sonst verpassen wir die.
     * 
     * @return {@link Ordered#HIGHEST_PRECEDENCE}
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
