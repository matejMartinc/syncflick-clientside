/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This code was generated by https://code.google.com/p/google-apis-client-generator/
 * (build: 2015-03-26 20:30:19 UTC)
 * on 2015-04-11 at 12:17:33 UTC 
 * Modify at your own risk.
 */

package mmdevelopment.syncserver.taskApi.model;

/**
 * Model definition for CategoryBean.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the taskApi. For a detailed explanation see:
 * <a href="http://code.google.com/p/google-http-java-client/wiki/JSON">http://code.google.com/p/google-http-java-client/wiki/JSON</a>
 * </p>
 *
 * @author Google, Inc.
 */
@SuppressWarnings("javadoc")
public final class CategoryBean extends com.google.api.client.json.GenericJson {

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.util.List<FlickBean> flickBeans;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String name;

  /**
   * @return value or {@code null} for none
   */
  public java.util.List<FlickBean> getFlickBeans() {
    return flickBeans;
  }

  /**
   * @param flickBeans flickBeans or {@code null} for none
   */
  public CategoryBean setFlickBeans(java.util.List<FlickBean> flickBeans) {
    this.flickBeans = flickBeans;
    return this;
  }

  /**
   * @return value or {@code null} for none
   */
  public java.lang.String getName() {
    return name;
  }

  /**
   * @param name name or {@code null} for none
   */
  public CategoryBean setName(java.lang.String name) {
    this.name = name;
    return this;
  }

  @Override
  public CategoryBean set(String fieldName, Object value) {
    return (CategoryBean) super.set(fieldName, value);
  }

  @Override
  public CategoryBean clone() {
    return (CategoryBean) super.clone();
  }

}
