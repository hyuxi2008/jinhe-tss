/*
 * ����˵������ֹѡȡ�ؼ��������¼�
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _onselectstart() {
	event.returnValue = false;
	return false;  
}

/*
 * ����˵�������˫����Ӧ�����������Զ���˫���¼���
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _ondblclick(){
	var eventObj = window.event.srcElement;
	var row = getRow(eventObj);
	if(row instanceof Row){
		var treeNode = instanceTreeNode(row.getXmlNode());
	}
	if(!(row instanceof Row) || !(treeNode instanceof TreeNode) || !treeNode.isCanSelected() || (eventObj != row.getLabel() && eventObj != row.getIcon())){	
		return;
	}
	//����˫���¼�
	var eventObj = createEventObject();
	eventObj.treeNode = treeNode;
	eventNodeDoubleClick.fire(eventObj);
}

/*
 * ����˵����	����Ҽ������¼���Ӧ����
 *			�����������������ӣ��򼤻�ýڵ㣬ͬʱ�����Ҽ������¼���
 * ������
 * ����ֵ��
 * ���ߣ�ë��
 * ʱ�䣺2006-5-7
 */
function _oncontextmenu(){
	var eventObj = window.event.srcElement;
	window.event.returnValue = false;
	var row = getRow(eventObj);
	if(row instanceof Row){
		var treeNode = instanceTreeNode(row.getXmlNode());
	}
	if(!(row instanceof Row) || !(treeNode instanceof TreeNode)){
		return;
	}
    //���ýڵ�Ϊ����
    if(true==treeNode.isCanSelected()){
        treeObj.setActiveNode(treeNode);
    }

    //�����Ҽ�����ڵ��¼�
    var eventObj = createEventObject();
    eventObj.treeNode = treeNode;
    eventObj.clientX = event.clientX;
    eventObj.clientY = event.clientY;
    eventNodeRightClick.fire(eventObj);

	displayObj.reload();
    
}

/*
 * ����˵����	��굥���¼���Ӧ����
 *			����������ѡ��״̬ͼ�꣬��ı�ѡ��״̬��ͬʱ����treeNodeSelectedChangeState���ԣ�ȷ���Ƿ�ͬʱ����ýڵ㡣
 *			��������������״̬ͼ�꣬��򿪻�������ǰ�ڵ��ֱϵ�ӽڵ㡣
 *			�����������������ӣ��򼤻�ýڵ㣬ͬʱ����treeNodeSelectedChangeState���ԣ�ȷ���Ƿ�ͬʱ�ı�ڵ�ѡ��״̬��
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _onclick(){
	var eventObj = window.event.srcElement;
	window.event.returnValue = false;
	var row = getRow(eventObj);
	if(row instanceof Row){
		var treeNode = instanceTreeNode(row.getXmlNode());
	}
	if(!(row instanceof Row) || !(treeNode instanceof TreeNode)){
		return;
	}
	if(eventObj == row.getCheckType()){		//���ݲ�ͬ��treeType���ı���Ӧ��ѡ��״̬
		treeNode.changeSelectedState(window.event.shiftKey);
	}else if(eventObj == row.getFolder()){
		treeNode.changeFolderState();		//չ���������ڵ��ֱϵ�ӽڵ�
	}else if(eventObj == row.getLabel() || eventObj == row.getIcon()){
		if(treeObj.isChangeFolderStateByClickLabel()){
			//2006-4-22 ֻ�е�֦�ڵ������ִ��
			if(treeNode.node.hasChildNodes()){
				//����ڵ�����ʱ���ı�ڵ�����״̬
				treeNode.changeFolderState();
			}
		}
		treeNode.setActive(window.event.shiftKey);		//����ڵ�
	}
	displayObj.reload();
}

//����Ƶ��ڵ���
/*
 * ����˵��������Ƶ�Ԫ���ϡ�
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _onmouseover(){
	var obj = window.event.srcElement;
	var row = getRow(obj);
	if(!(row instanceof Row) || row.getLabel() != obj){
		return;
	}
	row.setClassName(treeObj.getClassName(row.getXmlNode(), _TREE_NODE_OVER_STYLE_NAME));
}
//����뿪�ڵ�
/*
 * ����˵��������뿪Ԫ��ʱ��
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _onmouseout(){
	var obj = window.event.srcElement;
	var row = getRow(obj);
	if(!(row instanceof Row) || row.getLabel() != obj){
		return;
	}
	row.setClassName(treeObj.getClassName(row.getXmlNode()));
}

///////////////////////	���º������ڽڵ��϶� ////////////////////////////

/*
 * ����˵������ʼ�϶��¼���Ӧ���趨�϶��ڵ�
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _ondragstart(){
	if(!treeObj.isCanMoveNode()){		//ȷ���Ƿ��ṩ�϶��ڵ㹦��
		return;
	}
	var obj = window.event.srcElement;
	var row = getRow(obj);
	if(!(row instanceof Row) || obj != row.getLabel()){
		return;
	}
	var node = row.getXmlNode();
	//�趨�϶��ڵ�
	treeObj.setMovedNode(node);

	//2006-4-7 ����Ϊȫ��
	//element.movedNode = node;
	//element.movedNodeScrollTop = displayObj.getScrollTop() + getTop(obj);
	//element.movedRow = obj;
	var tempData = {};
	tempData.moveTree = element;
	tempData.movedNode = node;
	tempData.movedNodeScrollTop = displayObj.getScrollTop() + getTop(obj);
	tempData.movedRow = obj;
	window._dataTransfer = tempData;

	row.setClassName(_TREE_NODE_MOVED_STYLE_NAME);
	window.event.dataTransfer.effectAllowed = "move";
}

/*
 * ����˵�����϶���ɣ������Զ���ڵ��϶��¼�
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _ondrop(){
	if(!treeObj.isCanMoveNode()){		//ͬ��
		return;
	}
	stopScrollTree();
	var obj = window.event.srcElement;
	obj.runtimeStyle.borderBottom = _TREE_NODE_MOVE_TO_HIDDEN_LINE_STYLE;
	obj.runtimeStyle.borderTop = _TREE_NODE_MOVE_TO_HIDDEN_LINE_STYLE;
	//�����Զ����¼�
	var eObj = createEventObject();
	eObj.movedTreeNode = instanceTreeNode(window._dataTransfer.movedNode);
	eObj.toTreeNode = instanceTreeNode(window._dataTransfer.toNode);
	eObj.moveState = window._dataTransfer.moveState;
	//2006-4-7 ���ӱ��϶��Ľڵ�������
	eObj.moveTree = window._dataTransfer.moveTree;
	eventNodeMoved.fire(eObj);
}

/*
 * ����˵�����϶�������ȥ���϶�ʱ���ӵ���ʽ
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function  _ondragend(){
	if(!treeObj.isCanMoveNode()){		//ͬ��
		return;
	}
	stopScrollTree();
	var obj = window.event.srcElement;
	var row = getRow(obj);
	if(!(row instanceof Row) || obj != row.getLabel()){
		return;
	}
	obj.runtimeStyle.borderBottom = _TREE_NODE_MOVE_TO_HIDDEN_LINE_STYLE;
	obj.runtimeStyle.borderTop = _TREE_NODE_MOVE_TO_HIDDEN_LINE_STYLE;
	treeObj.setMovedNode(null);
	displayObj.reload();
}

/*
 * ����˵�����϶�ʱ��������ڵ㣬�趨Ŀ��ڵ���϶�״̬
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _ondragenter(){
	if(!treeObj.isCanMoveNode()){		//ͬ��
		return;
	}
    if(null==window._dataTransfer){
        return;
    }
	var obj = window.event.srcElement;
	//�ж��Ƿ���Ҫ����������������Ӧ�Ĺ���
	startScrollTree(obj);
	var row = getRow(obj);
	if(row instanceof Row){
		var node = row.getXmlNode();
	}

	//2006-4-7 �����Ƿ�ͬһ����
	if(!(row instanceof Row)
		|| obj != row.getLabel()){		//�϶��Ĳ����������ӣ�����Ч
		return;
	}
	//��ͬһ����
	if(window._dataTransfer && window._dataTransfer.moveTree==element){
		if(window._dataTransfer.movedNode == null
			|| node.parentNode != window._dataTransfer.movedNode.parentNode	//�����ֵܽڵ���Ч
			|| obj == window._dataTransfer.movedRow){		//Ŀ��ڵ���ͬ��Ч
			return;
		}
	}else{
	}

	window._dataTransfer.toNode = node;
	if(displayObj.getScrollTop() + getTop(obj) > window._dataTransfer.movedNodeScrollTop){
		window._dataTransfer.moveState = 1;
		obj.runtimeStyle.borderBottom = _TREE_NODE_MOVE_TO_LINE_STYLE;
	}else{
		window._dataTransfer.moveState = -1;
		obj.runtimeStyle.borderTop = _TREE_NODE_MOVE_TO_LINE_STYLE;
	}
	window.event.returnValue = false;
	window.event.dataTransfer.dropEffect = "move";
}

/*
 * ����˵�����϶�ʱ������뿪�ڵ�
 * ������
 * ����ֵ��
 * ���ߣ�scq
 * ʱ�䣺2004-7-1
 */
function _ondragleave(){
	if(!treeObj.isCanMoveNode()){
		return;
	}
	stopScrollTree(obj);
	var obj = window.event.srcElement;
	var row = getRow(obj);
	if(!(row instanceof Row) || obj != row.getLabel()){
		return;
	}
	obj.runtimeStyle.borderBottom = _TREE_NODE_MOVE_TO_HIDDEN_LINE_STYLE;
	obj.runtimeStyle.borderTop = _TREE_NODE_MOVE_TO_HIDDEN_LINE_STYLE;
	window.event.dataTransfer.dropEffect = "none";
}
/////////////////////////////// �ڵ��϶����� /////////////////////////////