 /* 
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Petter Norgren
 * 9/11/2015
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;

import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.util.coord.MapTileUtil;


/**
 * @author petternorgren
 *
 */
@SuppressWarnings("deprecation")
@MapTileProvider(name = "Statens Kartverk (Norway)")
public class TileKartverket extends TileHttpFetcher {

    private static final long serialVersionUID = -7203527367652271594L;

    private static final String HOST = "openwms.statkart.no";

    protected static String tileClassId = TileKartverket.class.getSimpleName();
    
    private static Map<String, TileKartverket> tilesMap = Collections.synchronizedMap(new HashMap<String, TileKartverket>());

    private static boolean alreadyInitialize = false;
	
	private static final int MAX_LEVEL_OF_DETAIL = 20;
    
    public TileKartverket(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
        initialize();
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileKartverket(String id) throws Exception {
        super(id);
        initialize();
    }

    private synchronized void initialize() {
        if (alreadyInitialize)
            return;
        alreadyInitialize = true;
        httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(HOST)), 8); // was setMaxForRoute
    }
	
	public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }

    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
		
		int imgSize = 256;
		double[] imgBox = {0, 0, 0, 0};
		
        double[] ret = MapTileUtil.XYToDegrees(worldX, worldY, levelOfDetail);
		double[] ret2 = MapTileUtil.XYToDegrees(worldX + imgSize, worldY + imgSize, levelOfDetail);
		
		imgBox[0] = Math.min(ret[1], ret2[1]);
		imgBox[1] = Math.min(ret[0], ret2[0]);
		imgBox[2] = Math.max(ret[1], ret2[1]);
		imgBox[3] = Math.max(ret[0], ret2[0]);
		
		String chosenLayerName1 = "dybdedata";
		String chosenLayerName2 = "Dybdedata_MS_WMS";
		
        String urlGet = "http://" + HOST + "/skwms1/wms." + chosenLayerName1 + "?LAYERS=" + chosenLayerName2 
				+ "&TRANSPARENT=TRUE&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&SRS=EPSG%3A4326&BBOX=" 
				+ imgBox[0] + "," + imgBox[1] + "," + imgBox[2] + "," + imgBox[3] 
				+ "&WIDTH=" + imgSize + "&HEIGHT=" + imgSize;
        return urlGet;
    }
    
	/* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeMillisToSeparateConnections()
     */
    @Override
    protected long getWaitTimeMillisToSeparateConnections() {
        return (long) (10 * rnd.nextDouble());
    }
    
    /**
     * @return the tilesMap
     */
    @SuppressWarnings("unchecked")
    public static <T extends Tile> Map<String, T> getTilesMap() {
        return (Map<String, T>) tilesMap;
    }

    /**
     * 
     */
    public static void clearDiskCache() {
        Tile.clearDiskCache(tileClassId);
    }

    /**
     * @return 
     * 
     */
    public static <T extends Tile> Vector<T> loadCache() {
        return Tile.loadCache(tileClassId);
    }
}
