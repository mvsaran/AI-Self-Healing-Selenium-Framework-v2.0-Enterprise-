package com.aiheal.healing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * WebElementProxy intercepts calls made to a healed WebElement (like .click())
 * and automatically retries locating the element if a StaleElementReferenceException
 * occurs mid-action.
 */
public class WebElementProxy implements InvocationHandler {

    private static final Logger log = LogManager.getLogger(WebElementProxy.class);

    private final WebDriver driver;
    private final By activeLocator;
    private WebElement underlyingElement;

    private WebElementProxy(WebDriver driver, WebElement underlyingElement, By activeLocator) {
        this.driver = driver;
        this.underlyingElement = underlyingElement;
        this.activeLocator = activeLocator;
    }

    /**
     * Synthesizes and returns a Proxy instance masking the given WebElement.
     */
    public static WebElement createProxy(WebDriver driver, WebElement element, By locator) {
        return (WebElement) Proxy.newProxyInstance(
                WebElement.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                new WebElementProxy(driver, element, locator)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            // Forward action straight to the real element object
            return method.invoke(underlyingElement, args);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            
            // Auto-heal Stale Elements directly dynamically inside the same step sequence
            if (targetException instanceof StaleElementReferenceException) {
                log.warn("🔄 [StaleElementReferenceException] intercepted upon running '{}()'. " +
                         "Element detached from DOM, re-acquiring using stable locator '{}'",
                         method.getName(), activeLocator);

                try {
                    // Refind with HealingEngine directly (in case it breaks again simultaneously)
                    underlyingElement = HealingEngine.findElement(driver, activeLocator);
                    log.info("Element successfully re-found in new DOM state!");
                    
                    // Attempt the target invocation action exactly one more time
                    return method.invoke(underlyingElement, args);
                } catch (Exception reacquireException) {
                    log.error("Failed to re-acquire Stale Element. Exhausted retry capability.");
                    throw reacquireException;
                }
            }
            throw targetException;
        }
    }
}
